package com.cloudcard.photoDownloader

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper
import jakarta.annotation.PostConstruct
import kong.unirest.core.HttpResponse
import kong.unirest.core.Unirest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

@Component("TouchNetClient")
@ConditionalOnProperty(value = "HttpStorageService.httpClient", havingValue = "TouchNetClient")
class TouchNetClient implements HttpStorageClient {

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

    private String sessionId
    private static final int MAX_TRIES = 3
    private int attemptCount = 0
    private int RETRY_DELAY_MS = 10000

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

    @Override
    String getSystemName() {
        return "TouchNet"
    }

    @Override
    void putPhoto(String accountId, String photoBase64) {
        ensureSession()

        Map request = [
                SessionID: sessionId,
                AccountID: accountId,
                PhotoBase64: photoBase64,
                ForcePrintedFlag: false
        ]

        TouchNetResponse response = doApiRequest("Account Photo Approve", "account/photo/approve", request)
        if (!response.success) {
            String errorCode = response.json?.ResponseStatus?.ErrorCode
            String message   = extractMessage(response.json) ?: "Unknown TouchNet error"

            if (isRateLimitError(errorCode, message)) {
                // Retry loop inside TouchNetClient
                for (int i = 1; i <= MAX_TRIES; i++) {
                    log.warn("Rate limit hit, retrying in ${RETRY_DELAY_MS}ms (attempt $i/$MAX_TRIES)")
                    Thread.sleep(RETRY_DELAY_MS)
                    response = doApiRequest("Account Photo Approve", "account/photo/approve", request)
                    if (response.success) return
                }
                throw new RuntimeException("Rate limit exceeded after $MAX_TRIES attempts: $message")
            }
            else {
                // Bubble up so HttpStorageService marks as FailedPhotoFile
                // Persistent errors and unknown/random failures
                throw new RuntimeException(message)
            }
        }

        attemptCount = 0
    }

    @Override
    void close() {
        if (sessionId) {
            log.info("Closing TouchNet session")
            try {
                if (!operatorLogout(sessionId)) {
                    log.warn("Failed to logout of TouchNet API session $sessionId")
                }
            } catch (Exception e) {
                log.error("Error while logging out of TouchNet API session $sessionId", e)
            } finally {
                sessionId = null
            }
        }
    }

    private void ensureSession() {
        if (!sessionId) {
            sessionId = operatorLogin()
            if (!sessionId) {
                throw new RuntimeException("Failed to login to TouchNet API")
            }
        }
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

    static String extractMessage(Object jsonResponse) {
        try {
            def parsed = (jsonResponse instanceof String) ?
                    new JsonSlurper().parseText(jsonResponse) :
                    jsonResponse
            return parsed?.ResponseStatus?.Message?.toString()
        } catch (Exception e) {
            return null
        }
    }

    private static boolean isRateLimitError(String code, String message) {
        return code == "-1100" || message?.toLowerCase()?.contains("rate limit")
    }
}

class TouchNetResponse {
    boolean success
    Map json

    TouchNetResponse(HttpResponse<String> response) {
        json = new JsonSlurper().parseText(response.body) as Map
        success = json.Status == "OK"
    }
}