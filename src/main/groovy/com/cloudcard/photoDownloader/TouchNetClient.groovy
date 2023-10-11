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
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "TouchNetStorageService")
class TouchNetClient {

    static final Logger log = LoggerFactory.getLogger(TouchNetClient.class);

    @Value('${TouchNetClient.apiUrl}')
    String apiUrl

    @Value('${TouchNetClient.developerKey}')
    String developerKey

    @Value('${TouchNetClient.operatorId}')
    String operatorId

    @Value('${TouchNetClient.operatorPassword}')
    String operatorPassword

    @Value('${TouchNetClient.terminalId}')
    String terminalId

    @Value('${TouchNetClient.terminalType}')
    String terminalType

    @Value('${TouchNetClient.originId}')
    int originId

    //TODO @PostConstruct to make sure the config parameters are properly specified.

    @PostConstruct
    void init() {

        throwIfBlank(apiUrl, "The TouchNet API URL must be specified.")
        throwIfBlank(developerKey, "The TouchNet developer key must be specified.")
        throwIfBlank(operatorId, "The TouchNet operator id must be specified")
        throwIfBlank(operatorPassword, "The TouchNet operator password must be specified")
        throwIfBlank(terminalId, "The TouchNet terminal id must be specified")
        throwIfBlank("$originId", "The TouchNet origin id must be specified.")

        log.info("          TouchNet API URL : $apiUrl")
        log.info("    TouchNet Developer Key : ...${developerKey.substring(3, 6)}...")
        log.info("      TouchNet Operator ID : $operatorId")
        log.info("TouchNet Operator Password : ${operatorPassword.length() > 0 ? "......" : ""}")
        log.info("      TouchNet Terminal ID : $terminalId")
        log.info("        TouchNet Origin ID : $originId")
    }

    boolean apiOnline() {
        log.trace("Checking if $apiUrl is online...")
        HttpResponse<String> response = Unirest.get("$apiUrl/").asString();
        log.trace("API Online Response: $response.status $response.body")
        return response.status == 200
    }

    String operatorLogin() {
        Map request = [
            OperatorId: operatorId,
            PIN: operatorPassword,
            TerminalID: terminalId,
            OriginID: originId,
            TerminalType: terminalType
        ]

        String requestBody = new ObjectMapper().writeValueAsString(request)

        log.trace("OperatorLogin Request Body: $requestBody")

        HttpResponse<String> response = Unirest.post("$apiUrl/operator/login")
                .header("Content-Type", "application/json")
                .header("DevKey", developerKey)
                .body(requestBody)
                .asString() // todo make response json and map to an object.

        log.trace("OperatorLogin Response: $response.status $response.body")

        def json = new JsonSlurper().parseText(response.body)
        boolean success = json.Status == "OK"
        if (!success) {
            log.error("OperatorLogin Failed: $response.body")
            return null;
        }

        return json.Result
    }

    boolean operatorLogout(String sessionId) {
        Map request = [
                SessionID: sessionId,
                OperatorId: operatorId
        ]

        String jsonBody = new ObjectMapper().writeValueAsString(request)

        log.trace("OperatorLogout Request Body: $jsonBody")

        HttpResponse<String> response = Unirest.post("$apiUrl/operator/logout")
                .header("Content-Type", "application/json")
                .header("DevKey", developerKey)
                .body(jsonBody) //todo make json
                .asString() // todo make json and map to an object.

        log.trace("OperatorLogout Response Body: $response.status $response.body")

        def json = new JsonSlurper().parseText(response.body)
        boolean success = json.Status == "OK"
        if (!success) {
            log.error("OperatorLogout Failed: $response.body")
        }
        return success
    }

    boolean accountPhotoApprove(String sessionId, String accountId, String photoBase64) {
        Map request = [
            SessionID: sessionId,
            AccountID: accountId,
            PhotoBase64: photoBase64,
            ForcePrintedFlag: false
        ]

        String jsonBody = new ObjectMapper().writeValueAsString(request)

        log.trace("AccountPhotoApprove Request Body: $jsonBody")

        HttpResponse<String> response = Unirest.post("$apiUrl/account/photo/approve")
                //todo abstract the basics into a function that we can call that inserts thte devkey, the json content type header, converts a map to json,
                //and throws the exception in case of failure, and returns the response content as a json parsed into a map.
                .header("Content-Type", "application/json")
                .header("DevKey", developerKey)
                .body(jsonBody) //todo make json
                .asString() // todo make json and map to an object.

        log.trace("AccountPhotoApprove Response: $response.status $response.body")

        def json = new JsonSlurper().parseText(response.body)
        boolean success = json.Status == "OK"
        if (!success) {
            log.error("AccountPhotoApproveRequest Failed: $response.body")
        }
        return success
    }
}
