//file:noinspection SpellCheckingInspection
package com.cloudcard.photoDownloader.storage.integration.ccure

import com.cloudcard.photoDownloader.FailedPhotoFileException
import com.cloudcard.photoDownloader.IntegrationRateLimitExceededException
import com.github.signalr4j.client.hubs.HubConnection
import com.github.signalr4j.client.hubs.HubProxy
import groovy.json.JsonSlurper
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RateLimiterConfig
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
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

import java.time.Duration
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
    String baseUrl // http://<server-ip>/victorWebService
    static String apiUrl // http://<server-ip>/victorWebService/api

    @Value('${CCureClient.username}')
    String username

    @Value('${CCureClient.password}')
    String password

    @Value('${CCureClient.clientId}')
    String clientId

    @Value('${CCureClient.employeeIdField}')
    String employeeIdField

    static String currentSessionId
    static HubConnection hubConnection

    RateLimiter ccureRateLimiter

    @PostConstruct
    void init() {
        throwIfBlank(baseUrl, "The CCURE base url for the api must be provided in CCureClient.baseUrl")
        throwIfBlank(username, "The CCURE username for the api must be provided in CCureClient.username")
        throwIfBlank(password, "The CCURE password for the api must be provided in CCureClient.password")
        throwIfBlank(clientId, "The CCURE integration client ID must be provided in CCureClient.clientId")
        throwIfBlank(employeeIdField, "The CCURE integration custom employee ID field name must be provided in CCureClient.employeeIdField")
        apiUrl = "$baseUrl/api"

        //CCURE docs say to space out all calls by 1-2 seconds to avoid rate limiting, so this will in enforce a 1
        //second wait between calls
        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofSeconds(30))
                .build()
        this.ccureRateLimiter = RateLimiter.of("ccureApi", config)
    }

    @PreDestroy
    static void shutdown() {
        log.info("Shutting down CCURE connections...")
        if (hubConnection != null) {
            hubConnection.stop()
            log.info("Connection hub stopped. Logging out...")
        }

        // manual pause to avoid throttling, since this is a static method
        Thread.sleep(2000)

        HttpResponse<String> response = Unirest.post("${apiUrl}/Authenticate/Logout")
                    .header("session-id", currentSessionId)
                    .asString();

        if (response.isSuccess()) {
            log.info("Logged out successfully.");
        }
    }

    def throttledCall(Closure apiCall) {
        try {
            return RateLimiter.decorateCallable(ccureRateLimiter, apiCall).call()
        } catch (RequestNotPermitted ex) {
            throw new IntegrationRateLimitExceededException()
        }
    }

    String authenticate() {
        log.info("Authenitcating with CCURE...")
        HttpResponse<String> response = throttledCall {
            Unirest.post("${apiUrl}/authenticate/Login")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .field("UserName", username)
                    .field("Password", password)
                    .field("ClientName", "RemotePhoto Integration")
                    .field("ClientID", clientId)
                    .field("ClientVersion", "3.10")
                    .asString()
        } as HttpResponse<String>

        if (response.isSuccess()) {
            String sessionId = response.getHeaders().getFirst("session-id");
            log.info("Authenticated with CCURE.");
            currentSessionId =  sessionId;
            return currentSessionId
        } else {
            throw new RuntimeException("CCURE Login failed: " + response.getStatusText());
        }
    }

    @Scheduled(fixedRateString = '${CCureClient.keepAliveTime:1500000}')
    void refreshSession() {
        if (currentSessionId == null) return;

        try {
            HttpResponse<String> response = throttledCall {
                Unirest.post("${apiUrl}/v2/session/keepalive")
                        .header("session-id", currentSessionId)
                        .header("accept", "application/json")
                        .asString()
            } as HttpResponse<String>

            if (response.isSuccess()) {
                log.trace("Session refreshed. Server UTC: " + response.getBody());
            } else if (response.getStatus() == 401) {
                log.trace("Session expired or invalid ID used.");
                authenticate()
            }
        } catch (IntegrationRateLimitExceededException e) {
            // ignore, we're already sending enough requests to keep the connection alive
        } catch (Exception e) {
            authenticate()
        }
    }

    Long createPersonnel(String firstName, String lastName, String email, String employeeId) {
        if (!firstName || !lastName) {
            throw new FailedPhotoFileException("First/Last Name fields were missing for $email; unable to create CCURE Personnel")
        }

        HttpResponse<String> response = throttledCall {
            Unirest.post("${apiUrl}/Objects/Persist")
                    .header("session-id", currentSessionId)
                    .field("Type", CCurePersonnel.TYPE)
                    .field("PropertyNames[0]", "FirstName")
                    .field("PropertyValues[0]", firstName)
                    .field("PropertyNames[1]", "LastName")
                    .field("PropertyValues[1]", lastName)
                    .field("PropertyNames[2]", "EmailAddress")
                    .field("PropertyValues[2]", email)
                    .field("PropertyNames[3]", employeeIdField)
                    .field("PropertyValues[3]", employeeId)
                    .asString()
        } as HttpResponse<String>

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

    void storePhoto(Long identifier, String base64Image, Long partition, boolean isPrimaryPortrait) {
        if (isPrimaryPortrait) {
            removePrimaryPhoto(identifier)
        } else {
            def photoId = findExistingSignaturePhotoId(identifier)
            if (photoId) {
                deletePhoto(photoId)
            }
        }


        log.info "Sending photo to CCURE for $identifier"

        String imageType = isPrimaryPortrait ? "1" : "2"  // 1 = Portrait, 2 = signature

        Map<String, Object> fields = [
                "Type"                                : CCurePersonnel.TYPE,
                "ID"                                  : identifier,
                "Children[0].Type"                    : "SoftwareHouse.NextGen.Common.SecurityObjects.Images",
                // Note: Field names for arrays in v1 form posts often need explicit indexing
                "Children[0].PropertyNames[0]"        : "Name",
                "Children[0].PropertyNames[1]"        : "ParentId",
                "Children[0].PropertyNames[2]"        : "ImageType",
                "Children[0].PropertyNames[3]"        : "PartitionID",
                "Children[0].PropertyNames[4]"        : "Primary",
                "Children[0].PropertyNames[5]"        : "Image",
                "Children[0].PropertyNames[6]"        : "ImageCaptureDate",

                "Children[0].PropertyValues[0]"       : "Portrait_${imageType}_${identifier}",
                "Children[0].PropertyValues[1]"       : identifier,
                "Children[0].PropertyValues[2]"       : imageType,
                "Children[0].PropertyValues[3]"       : partition?.toString() ?: "1",
                "Children[0].PropertyValues[4]"       : isPrimaryPortrait ? "true" : "false",
                "Children[0].PropertyValues[5]"       : base64Image,
                "Children[0].PropertyValues[6]"       : LocalDateTime.now().format(LastRunPropertyServiceImpl.CCURE_DATE_FORMATTER),
        ]

        HttpResponse<String> response = throttledCall {
            Unirest.post("${apiUrl}/Objects/PersistToContainer")
                    .header("session-id", currentSessionId)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .fields(fields)
                    .asString()
        } as HttpResponse<String>

        if (!response.success) {
            throw new FailedPhotoFileException("Failed to upload photo: ${response.status} - ${response.body}")
        }

        log.trace "Photo uploaded successfully for Personnel ID: ${identifier}"
    }

    List<CCurePersonnel> queryAuditLogsForNewPeople(int pageNumber = 1, List<CCurePersonnel> resultList = []) {
        String lastRun = lastRunPropertyService.lastRunTimestamp
        log.info("Check CCURE audit logs (page $pageNumber) for personnel created since $lastRun ...")

        HttpResponse<JsonNode> response = throttledCall {
            Unirest.post(apiUrl + "/v2/Audit/FindInAudit")
                    .header("session-id", currentSessionId)
                    .header("Content-Type", "application/json")
                    .body(new AuditRequest(
                            lastRun,
                            LocalDateTime.now().plusDays(1).format(LastRunPropertyServiceImpl.CCURE_DATE_FORMATTER),
                            PAGE_SIZE,
                            pageNumber
                    ))
                    .asJson()
        } as HttpResponse<JsonNode>

        if (response.isSuccess()) {
            JSONArray responseItems = response.node.jsonArray
            if (!responseItems.empty) {
                Object metaData = responseItems.get(0)
                responseItems.remove(0)
                log.info("Audit Records Found: " + responseItems.size());
                responseItems.each {
                    // Ignore any creation records for photos, cards, etc., that may be attached to a person as a secondary object.
                    if (it.SecondaryObjectType == "") {
                        resultList << getPersonnelDetailsByGuid(it.PrimaryObjectIdentity)
                    }
                }

                // use the metadata to recursively fetch more pages if needed
                if (metaData.TotalPages > pageNumber) {
                    return queryAuditLogsForNewPeople(pageNumber + 1, resultList)
                } else {
                    log.info("Processing ${resultList.size()} personnel creation records. Discarding any secondary object records.")
                    return resultList
                }
            }
        } else {
            log.trace("Error querying audit: " + response.toString());
        }
    }

    /**
     * Be careful with overusing these calls. They include the encoded photo 3 times (full image, thumbnail, and a
     * small 3rd version), so the response is big and potentially slow.
     * @param guid
     * @return
     */
    CCurePersonnel getPersonnelDetailsByGuid(String guid) {
        MultipartBody request = Unirest.post(apiUrl + "/Objects/FindObjsWithCriteriaFilter")
                .field("TypeFullName", CCurePersonnel.TYPE)
                .field("whereClause", "GUID = ?")
                .field("arguments[]", guid)
                .field("ArgTypes[]", "GUID") // Mandatory when passing a GUID argument
                .field("PageSize", "1");

        return executePersonnelSearch(request);
    }

    /**
     * Be careful with overusing these calls. They include the encoded photo 3 times (full image, thumbnail, and a
     * small 3rd version), so the response is big and potentially slow.
     * @param guid
     * @return
     */
    CCurePersonnel getPersonnelDetails(String id, String email) {
        CCurePersonnel result = getPersonnelDetailsByEmployeeId(id)
        if (!result) {
            result = getPersonnelDetailsByEmail(email)
        }

        return result
    }

    CCurePersonnel getPersonnelDetailsByEmployeeId(String id) {
        MultipartBody request = Unirest.post(apiUrl + "/Objects/FindObjsWithCriteriaFilter")
                .field("TypeFullName", CCurePersonnel.TYPE)
                .field("whereClause", "$employeeIdField = ?")
                .field("arguments[]", id) // ArgTypes[] is not required for standard string comparisons
                .field("PageSize", "1");

        return executePersonnelSearch(request);
    }

    CCurePersonnel getPersonnelDetailsByEmail(String email) {
        MultipartBody request = Unirest.post(apiUrl + "/Objects/FindObjsWithCriteriaFilter")
                .field("TypeFullName", CCurePersonnel.TYPE)
                .field("whereClause", "EmailAddress = ?")
                .field("arguments[]", email) // ArgTypes[] is not required for standard string comparisons
                .field("PageSize", "1");

        return executePersonnelSearch(request);
    }

    private CCurePersonnel executePersonnelSearch(MultipartBody request) {
        HttpResponse<JsonNode> response = throttledCall {
            request
                    .header("session-id", currentSessionId)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .asJson()
        } as HttpResponse<JsonNode>

        if (response.isSuccess()) {
            var jsonArray = response.getBody().getArray();

            if (jsonArray.length() == 0) {
                return null;
            }

            var responseObj = jsonArray.getJSONObject(0);

            return new CCurePersonnel(
                    id: responseObj.optInt("ObjectID"),
                    emailAddress: responseObj.optString("EmailAddress"),
                    employeeId: responseObj.optString(employeeIdField),
                    partitionId: responseObj.optInt("PartitionID")
            );
        } else {
            log.error("Error fetching details: " + response.getStatus() + " - " + response.getStatusText());
            return null;
        }
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

    // This accomplishes a delete in 1 call instead of two, but only works on primary photos
    // Signatures will use a 2-call method, which is slower because of the throttling
    void removePrimaryPhoto(Long personIdentifier) {
        HttpResponse<String> response = throttledCall {
            Unirest.post("${apiUrl}/Generic/ExecuteCrossfireMethod")
                    .header("session-id", currentSessionId)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .queryString("requestService", "SoftwareHouse.NextGen.Common.SecurityObjects.Images")
                    .queryString("methodName", "DeleteImagePrimaryForPerson")
                    .field("ArgumentValues", "{ \"personId\" : $personIdentifier }")
                    .asString()
        } as HttpResponse<String>

        if (response.success && response.body == "true") {
            log.trace("Removed primary photo for $personIdentifier")
        } else {
            log.warn("Unable to remove photo for $personIdentifier : $response.status")
        }
    }

    private findExistingSignaturePhotoId(long personIdentifier) {
        log.trace "Attempting to find photo for person: $personIdentifier"

        // Find the ObjectID of the current Primary Image
        // Use indexed arrays for Arguments and DisplayProperties to ensure CCURE parses them correctly
        Map<String, Object> findFields = [
                "TypeFullName"        : "SoftwareHouse.NextGen.Common.SecurityObjects.Images",
                "DisplayProperties[0]": "ObjectID",
                "WhereClause"         : "ParentID = ? AND ImageType = ? AND Primary = ?",
                "Arguments[0]"        : personIdentifier.toString(),
                "Arguments[1]"        : "2", // 2 = Signature
                "Arguments[2]"        : "false"
        ]

        HttpResponse<String> findResponse = throttledCall {
            Unirest.post("${apiUrl}/Objects/GetAllWithCriteria")
                    .header("session-id", currentSessionId)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .fields(findFields)
                    .asString()
        } as HttpResponse<String>

        if (!findResponse.success || findResponse.body == "[]" || findResponse.body == "null") {
            log.warn "No primary photo found for person $personIdentifier"
            return null
        }

        def images = new JsonSlurper().parseText(findResponse.body)
        return images[0].ObjectID
    }

    private deletePhoto(Long imageObjectId) {
        HttpResponse<String> response = throttledCall {
            Unirest.delete("${apiUrl}/Objects/Delete")
                    .header("session-id", currentSessionId)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .queryString("type", "SoftwareHouse.NextGen.Common.SecurityObjects.Images")
                    .queryString("id", imageObjectId)
                    .asString()
        } as HttpResponse<String>

        if (!response.isSuccess()) {
            log.warn "Unable to delete photo id $imageObjectId"
        } else {
            log.info "Deleted photo $imageObjectId"
        }
    }
}
