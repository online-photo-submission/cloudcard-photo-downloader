//file:noinspection SpellCheckingInspection
package com.cloudcard.photoDownloader.storage.integration.ccure

import com.github.signalr4j.client.hubs.HubConnection
import com.github.signalr4j.client.hubs.HubProxy
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import kong.unirest.core.HttpRequestWithBody
import kong.unirest.core.HttpResponse
import kong.unirest.core.JsonNode
import kong.unirest.core.MultipartBody
import kong.unirest.core.Unirest
import kong.unirest.core.json.JSONArray
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

import java.time.LocalDateTime

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

@Component
@ConditionalOnProperty(value = "IntegrationStorageService.client", havingValue = "CCureClient")
class CCureClient {

    static final Logger log = LoggerFactory.getLogger(CCureClient.class)
    static final int PAGE_SIZE = 100

    @Autowired
    LastRunPropertyService lastRunPropertyService

    @Value('${CCureClient.baseUrl}')
    static String baseUrl // http://<server-ip>/victorWebService
    static String apiUrl = "$baseUrl/api"// http://<server-ip>/victorWebService/api

    @Value('${CCureClient.username}')
    String username

    @Value('${CCureClient.password}')
    String password

    @Value('${CCureClient.clientId}')
    String clientId

    @Value('${CCureClient.partition:1}')
    String partition

    @Value('${CCureClient.employeeIdField}')
    String employeeIdField

    static String currentSessionId
    static HubConnection hubConnection

    //TODO: add throttling backoff to the CCURE api calls

    @PostConstruct
    void init() {
        throwIfBlank(baseUrl, "The CCURE base url for the api must be provided in CCureClient.baseUrl")
        throwIfBlank(username, "The CCURE username for the api must be provided in CCureClient.username")
        throwIfBlank(password, "The CCURE password for the api must be provided in CCureClient.password")
        throwIfBlank(clientId, "The CCURE integration client ID must be provided in CCureClient.clientId")
    }

    @PreDestroy
    static void shutdown() {
        log.info("Shutting down CCURE connections...")
        if (hubConnection != null) {
            hubConnection.stop()
            log.info("Connection hub stopped. Logging out...")
        }

        HttpResponse<String> response = Unirest.post("${apiUrl}/Authenticate/Logout")
                .header("session-id", currentSessionId)
                .asString();

        if (response.isSuccess()) {
            log.info("Logged out successfully.");
        }
    }

    String authenticate() {
        log.info("Authenitcating with CCURE...")
        HttpResponse<String> response = Unirest.post("${apiUrl}/authenticate/Login")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .field("UserName", username)
                .field("Password", password)
                .field("ClientName", "RemotePhoto Integration")
                .field("ClientID", clientId)
                .field("ClientVersion", "3.10")
                .asString();

        if (response.isSuccess()) {
            String sessionId = response.getHeaders().getFirst("session-id");
            log.info("Authenticated with CCURE.");
            currentSessionId =  sessionId;
            return currentSessionId
        } else {
            throw new RuntimeException("Login failed: " + response.getStatusText());
        }
    }

    @Scheduled(fixedRateString = '${CCureClient.keepAliveTime:1500000}')
    void refreshSession() {
        if (currentSessionId == null) return;

        try {
            HttpResponse<String> response = Unirest.post("${apiUrl}/v2/session/keepalive")
                    .header("session-id", currentSessionId)
                    .header("accept", "application/json")
                    .asString();

            if (response.isSuccess()) {
                log.trace("Session refreshed. Server UTC: " + response.getBody());
            } else if (response.getStatus() == 401) {
                log.trace("Session expired or invalid ID used.");
                init()
            }
        } catch (Exception e) {
            init()
        }
    }

    Long createPersonnel(String firstName, String lastName, String email) {
        def response = Unirest.post("${apiUrl}/Objects/Persist")
                .header("session-id", currentSessionId)
                .field("Type", "SoftwareHouse.NextGen.Common.SecurityObjects.Personnel")
                .field("PropertyNames[0]", "FirstName")
                .field("PropertyValues[0]", firstName)
                .field("PropertyNames[1]", "LastName")
                .field("PropertyValues[1]", lastName)
                .field("PropertyNames[2]", "EmailAddress")
                .field("PropertyValues[2]", email)
                .asString()

        if (response.isSuccess()) {
            return extractObjectId(response.body)
        } else {
            return null
        }
    }

    Long extractObjectId(String responseText) {
        if (!responseText) return null

        // Regex explanation:
        // is: '   -> Matches the literal text before the ID
        // (\d+)   -> Capturing group for one or more digits
        // '       -> Matches the closing single quote
        def matcher = responseText =~ /is: '(\d+)'/

        if (matcher.find()) {
            String idString = matcher.group(1)
            log.trace("Successfully extracted ObjectID: {}", idString)
            return idString.toLong()
        } else {
            log.warn("Could not find ObjectID in response: {}", responseText)
            return null
        }
    }

    void storePhoto(Long identifier, String base64Image) {
        log.trace "Sending photo to CCURE for $identifier"

        Map payload = [
            Type    : "SoftwareHouse.NextGen.Common.SecurityObjects.Personnel",
            ID      : identifier,
            Children: [
                [
                    Type          : "SoftwareHouse.NextGen.Common.SecurityObjects.Images",
                    PropertyNames : ["Name", "ParentId", "ImageType", "PartitionID", "Primary", "Image"],
                    PropertyValues: [
                            "Portrait_${identifier}", // Name
                            identifier,               // ParentId
                            "1",                       // ImageType (1 = Portrait)
                            partition,                       // PartitionID (Default)
                            "true",                    // Primary
                            base64Image                // Base64 String
                    ]
                ]
            ]
        ]

        // Step 3: Execute the POST request
        HttpResponse<String> response = Unirest.post("${apiUrl}/Objects/PersistToContainer")
                .header("session-id", currentSessionId)
                .header("Content-Type", "application/json")
                .body(payload)
                .asString()

        if (!response.success) {
            throw new RuntimeException("Failed to upload photo: ${response.status} - ${response.body}")
        }

        log.trace "Photo uploaded successfully for Personnel ID: ${identifier}"
    }

    List<CCurePersonnel> queryAuditLogsForNewPeople(int pageNumber = 1, List<CCurePersonnel> resultList = []) {
        String lastRun = lastRunPropertyService.lastRunTimestamp
        log.info("Check CCURE audit logs (page $pageNumber) for personnel created since $lastRun ...")

        HttpResponse<JsonNode> response = Unirest.post(apiUrl + "/v2/Audit/FindInAudit")
                .header("session-id", currentSessionId)
                .header("Content-Type", "application/json")
                .body(new AuditRequest(
                        lastRun,
                        LocalDateTime.now().plusDays(1).format(LastRunPropertyServiceImpl.CCURE_DATE_FORMATTER),
                        PAGE_SIZE,
                        pageNumber
                ))
                .asJson();

        if (response.isSuccess()) {
            JSONArray responseItems = response.node.jsonArray
            if (!responseItems.empty) {
                Object metaData = responseItems.get(0)
                responseItems.remove(0)
                log.info("Audit Records Found: " + responseItems.size());
                responseItems.each {
                    resultList << getPersonnelDetailsByGuid(it.PrimaryObjectIdentity)
                }

                // use the metadata to recursively fetch more pages if needed
                if (metaData.TotalPages > pageNumber) {
                    return queryAuditLogsForNewPeople(pageNumber + 1, resultList)
                } else {
                    return resultList
                }
            }
        } else {
            log.trace("Error querying audit: " + response.toString());
        }
    }

    CCurePersonnel executePersonnelSearch(MultipartBody request) {
        HttpResponse<JsonNode> response = request
                .header("session-id", currentSessionId)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .asJson();

        if (response.isSuccess()) {
            var jsonArray = response.getBody().getArray();

            if (jsonArray.length() == 0) {
                return null;
            }

            var responseObj = jsonArray.getJSONObject(0);

            return new CCurePersonnel(
                    id: responseObj.optInt("ObjectID"),
                    emailAddress: responseObj.optString("EmailAddress")
            );
        } else {
            log.error("Error fetching details: " + response.getStatus() + " - " + response.getStatusText());
            return null;
        }
    }

    CCurePersonnel getPersonnelDetailsByGuid(String guid) {
        MultipartBody request = Unirest.post(apiUrl + "/Objects/FindObjsWithCriteriaFilter")
                .field("TypeFullName", "SoftwareHouse.NextGen.Common.SecurityObjects.Personnel")
                .field("whereClause", "GUID = ?")
                .field("arguments[]", guid)
                .field("ArgTypes[]", "GUID") // Mandatory when passing a GUID argument [cite: 615]
                .field("PageSize", "1");

        return executePersonnelSearch(request);
    }

    CCurePersonnel getPersonnelDetailsByEmail(String email) {
        MultipartBody request = Unirest.post(apiUrl + "/Objects/FindObjsWithCriteriaFilter")
                .field("TypeFullName", "SoftwareHouse.NextGen.Common.SecurityObjects.Personnel")
                .field("whereClause", "EmailAddress = ?")
                .field("arguments[]", email)
        // ArgTypes[] is not required for standard string comparisons [cite: 614]
                .field("PageSize", "1");

        return executePersonnelSearch(request);
    }

    void subscribeForNewEvents(Closure handler) {
        if (hubConnection != null) {
            hubConnection.stop()
        }

        log.info("Subscribing to new CCURE personnel events...")

        hubConnection = new HubConnection("${baseUrl}/signalr")
        hubConnection.addHeader("Cookie", "hint=" + currentSessionId + ";path=/")

        HubProxy proxy = hubConnection.createHubProxy("TypeNotificationHub");

        proxy.on("notifyTypeMessage", handler, Object.class)

        hubConnection.start().get()

        // Subscribe specifically to Personnel objects
        proxy.invoke("subscribeForNotifications", currentSessionId, CCurePersonnel.TYPE, [])

        log.info("Subscription established.")
    }

}
