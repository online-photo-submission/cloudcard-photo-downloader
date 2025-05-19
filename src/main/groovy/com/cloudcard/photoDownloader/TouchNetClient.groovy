package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest
import groovy.json.JsonSlurper;

import jakarta.annotation.PostConstruct

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

    TouchNetResponse doApiRequest(String name, String endpoint, Map body) {
        String serializedBody = new ObjectMapper().writeValueAsString(body)

        log.trace("$name Request Body: $serializedBody")

        HttpResponse<String> response = Unirest.post("$apiUrl/$endpoint")
                .header("Content-Type", "application/json")
                .header("DevKey", developerKey)
                .body(serializedBody)
                .asString()

        log.trace("$name Response: $response.status $response.body")

        TouchNetResponse touchNetResponse = new TouchNetResponse(response)
        if (!touchNetResponse.success) {
            log.error("$name Failed: $response.body")
        }

        return touchNetResponse
    }

    String operatorLogin() {
        Map request = [
            OperatorId: operatorId,
            PIN: operatorPassword,
            TerminalID: terminalId,
            OriginID: originId,
            TerminalType: terminalType
        ]

        TouchNetResponse response = doApiRequest("Operator Login", "operator/login", request)

        return response.success ? response.json.Result : null
    }

    boolean operatorLogout(String sessionId) {
        Map request = [
            SessionID: sessionId,
            OperatorId: operatorId
        ]

        TouchNetResponse response = doApiRequest("Operator Logout", "operator/logout", request)

        return response.success
    }

    boolean accountPhotoApprove(String sessionId, String accountId, String photoBase64) {
        Map request = [
            SessionID: sessionId,
            AccountID: accountId,
            PhotoBase64: photoBase64,
            ForcePrintedFlag: false
        ]

        TouchNetResponse response = doApiRequest("Account Photo Approve", "account/photo/approve", request)

        return response.success
    }
}

class TouchNetResponse {
    boolean success
    Object json

    TouchNetResponse(HttpResponse<String> response) {
        json = new JsonSlurper().parseText(response.body)
        success = json.Status == "OK"
    }
}