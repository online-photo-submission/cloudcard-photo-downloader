package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest
import groovy.json.JsonSlurper;

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank;

@Component
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "CCureStorageService")
class CCureClient {

    static final Logger log = LoggerFactory.getLogger(CCureClient.class);

    @Value('${CCureClient.apiUrl}')
    String apiUrl

    @Value('${CCureClient.operatorId}')
    String operatorId

    @Value('${CCureClient.operatorPassword}')
    String operatorPassword

    //Defaults to CloudCard in app.props
    @Value('${CCureClient.clientName}')
    String clientName

    @Value('${CCureClient.clientId}')
    String clientId

    @Value('${CCureClient.compatibleCCureVersion}')
    String compatibleCCureVersion

    String sessionId

    @PostConstruct
    void init() {

        throwIfBlank(apiUrl, "The CCure API URL must be specified.")
        throwIfBlank(operatorId, "The CCure operator id must be specified")
        throwIfBlank(operatorPassword, "The CCure operator password must be specified")
        throwIfBlank(clientName, "The CCure Client Name must be specified")
        throwIfBlank(clientId, "The CCure Client id must be specified")
        throwIfBlank(compatibleCCureVersion, "The compatible CCure version must be specified.")

        log.info("           CCure API URL : $apiUrl")
        log.info("       CCure Operator ID : $operatorId")
        log.info(" CCure Operator Password : ${operatorPassword.length() > 0 ? "......" : ""}")
        log.info("       CCure Client Name : $clientName")
        log.info("         CCure Client ID : $clientId")
        log.info("Compatible CCure Version : $compatibleCCureVersion")

    }

    boolean isOnline() {
        log.trace("Checking if $apiUrl is online...")
        HttpResponse<String> response = Unirest.get("$apiUrl/").asString();
        log.trace("API Online Response: $response.status $response.body")
        return response.status == 200
    }

    CCureResponse doApiRequest(String name, String endpoint, Map body) {
        String serializedBody = new ObjectMapper().writeValueAsString(body)

        log.trace("$name Request Body: $serializedBody")


        //TODO the login request uses this method but needs to not pass sessionId.
        HttpResponse<String> response = Unirest.post("$apiUrl/$endpoint")
                .header("Content-Type", "application/json")
                .header("session-id", sessionId)
                .body(serializedBody)
                .asString()

        log.trace("$name Response: $response.status $response.body")

        CCureResponse cCureResponse = new CCureResponse(response)
        if (!cCureResponse.success) {
            log.error("$name Failed: $response.body")
        }

        return cCureResponse
    }

    String operatorLogin() {
        Map request = [
            UserName: operatorId, // todo rename params
            Password: operatorPassword,
            ClientName: clientName,
            ClientId: clientId, // todo add this as an argument - unique GUID that has been licensed for our integration.
            ClientVersion: compatibleCCureVersion //version number of CCure that this integration connects to.

        ]

        //TODO encode the above as form encoding, not json.
        CCureResponse response = doApiRequest("Operator Login", "api/authenticate/Login", request)

        //the sessionID comes from the response HEADER "session-id"
        sessionId = response.headers."session-id" //todo make this actually get the header value.

        // if not successful, throw an exception to stop execution.

        return response.success ? response.json.Result : null
    }

    /*
    boolean uploadPhoto(String accountId, String photoBase64) {
        if (!sessionId) {
            operatorLogin()
        }

        POST /api/Objects/PersistToContainer



        Map request = [
            Type: "SoftwareHouse.NextGen.Common.SecurityObjects.Personnel",
            ID: accountId,
            Children: [
                [
//                    type: "SoftwareHouse.NextGen.Common.SecurityObjects.Images"
                    PropertyNames: ["Type", "Name", "ParentId", "ImageType", "Primary", "Image"],
                    PropertyValues: [
                        "SoftwareHouse.NextGen.Common.SecurityObjects.Images",
                        "myPortrait",
                        accountId,
                        "1",
                        "true",
                        photoBase64
                    ]
                ]
            ]

        ]
        //TODO this somehow needs to be converted to form encoding where each element is one line. BLEAGH




        CCureResponse response = doApiRequest("Account Photo Approve", "account/photo/approve", request)

        return response.success
    }

    */
}

class CCureResponse {
    boolean success
    Object json

    CCureResponse(HttpResponse<String> response) {
        response.getHeaders().getFirst('session-id')
//        json = new JsonSlurper().parseText(response.body)
//        success = json.Status == "OK"
        success = false
    }
}