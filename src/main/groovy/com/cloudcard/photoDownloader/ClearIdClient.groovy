package com.cloudcard.photoDownloader

import groovy.json.JsonSlurper
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import java.nio.charset.StandardCharsets

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

@Component
@ConditionalOnProperty(value = "IntegrationStorageService.client", havingValue = "ClearIdClient")
class ClearIdClient implements IntegrationStorageClient {

    static final Logger log = LoggerFactory.getLogger(ClearIdClient.class)

    @Value('${ClearIdClient.apiUrl}')
    String apiUrl

    @Value('${ClearIdClient.stsUrl}')
    String stsUrl

    @Value('${ClearIdClient.accountId}')
    String accountId

    @Value('${ClearIdClient.clientId}')
    String clientId

    @Value('${ClearIdClient.clientSecret}')
    String clientSecret

    HttpClient client
    Retry retry

    String authToken

    @PostConstruct
    void init() {
        throwIfBlank(apiUrl, "The ClearId API URL must be specified.")
        throwIfBlank(accountId, "The ClearId accountId must be specified")
        throwIfBlank(clientId, "The ClearId clientId must be specified")
        throwIfBlank(clientSecret, "The ClearId clientSecret must be specified")

        log.info("     ClearId STS URL : $stsUrl")
        log.info("     ClearId API URL : $apiUrl")
        log.info("   ClearId accountId : $accountId")
        log.info("    ClearId clientId : $clientId")
        log.info("ClearId clientSecret : ${clientSecret.length() > 0 ? "......" : ""}")

        client = HttpClient.newBuilder().build()

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(10)
            .intervalFunction(IntervalFunction.ofExponentialBackoff(500, 2.0, 30_000))
            .retryExceptions(IntegrationRateLimitExceededException.class)
            .failAfterMaxAttempts(true)
            .build()

        retry = Retry.of("ClearIdClient", retryConfig)
    }

    @Override
    String getSystemName() {
        return "ClearId"
    }

    HttpResponse<String> sendWithBackoff(HttpRequest request) {
        return Retry.decorateSupplier(retry, {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 429) {
                log.trace("got a 429!")
                throw new IntegrationRateLimitExceededException()
            }

            return response
        }).get()
    }

    String getIdentity(String identifier) {
        HttpRequest request = authenticatedRequestBuilder(new URI("$apiUrl/accounts/$accountId/identities?ExternalId=$identifier"))
                .header("Accept", "application/json")
                .build()

        HttpResponse<String> response = sendWithBackoff(request)

        log.trace("ClearId GET /identities?ExternalId=$identifier : ${response.statusCode()}")

        if (response.statusCode() == 404) {
            throw new FailedPhotoFileException("ClearID identity not found for $identifier")
        }

        if (response.statusCode() != 200) {
            throw new Exception("Error finding ClearID identity for $identifier. Status: ${response.statusCode()}")
        }

        def body = new JsonSlurper().parseText(response.body())

        log.trace("ClearId GET /identities?ExternalId=$identifier : returned ${body?.identities?.size()} results")

        if (!body.identities || body.identities.size() == 0) {
            throw new FailedPhotoFileException("ClearID identity not found for $identifier")
        }

        if (body.identities.size() > 1) {
            throw new FailedPhotoFileException("ClearID contains multiple users with ExternalID $identifier")
        }
        String identityId = body.identities[0].identityId

        log.trace("ClearId GET /identities?ExternalId=$identifier : identityId: $identityId")

        return identityId
    }


    void putIdentityPicture(String identityId, byte[] photoBytes) {
        log.trace("ClearId PUT /identities/$identityId/picture : create multipart form with a picture size of $photoBytes.length bytes")

        String boundary = UUID.randomUUID().toString()
        byte[] separator = "--${boundary}\r\nContent-Disposition: form-data; name=\"picture\"; filename=\"picture.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n".getBytes()
        byte[] endBoundary = "\r\n--${boundary}--\r\n".getBytes()

        //we may still need this one though.
        byte[] body = new byte[separator.length + photoBytes.length + endBoundary.length]
        System.arraycopy(separator, 0, body, 0, separator.length)
        System.arraycopy(photoBytes, 0, body, separator.length, photoBytes.length)
        System.arraycopy(endBoundary, 0, body, separator.length + photoBytes.length, endBoundary.length)

        log.trace("ClearId PUT /identities/$identityId/picture : sending request with body size of $body.length bytes")

        HttpRequest request = authenticatedRequestBuilder(new URI("$apiUrl/accounts/$accountId/identities/$identityId/picture"))
                .header("Content-Type", "multipart/form-data; boundary=${boundary}")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build()

        HttpResponse<String> response = sendWithBackoff(request)

        log.trace("ClearId PUT /identities/$identityId/picture : status : ${response.statusCode()}")

        if (response.statusCode() != 200) {
            throw new Exception("Error updating picture for ClearId identity $identityId. Status: ${response.statusCode()}")
        }
    }

    @Override
    void putPhoto(String identifier, byte[] photoBytes) {
        putIdentityPicture(getIdentity(identifier), photoBytes)
    }

    @Override
    void close() {
        //Intentionally not implemented at present.
    }

    private HttpRequest.Builder authenticatedRequestBuilder(URI uri) {
        return HttpRequest.newBuilder()
            .uri(uri)
            .header("Authorization", "Bearer ${getValidToken()}")
    }

    String getValidToken() {
        authToken ?: authenticate()
    }

    private String authenticate() {
        Map<String, String> formData = [
            "client_id"    : clientId,
            "client_secret": clientSecret,
            "grant_type"   : "client_credentials"
        ]

        String tokenUrl = "$stsUrl/connect/token"

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(buildFormDataPublisher(formData))
            .build()

        log.trace("POST \"$stsUrl/connect/token\": Sending...")

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

        log.trace("POST \"$stsUrl/connect/token\": ${response.statusCode()}")

        Object json = new JsonSlurper().parseText(response.body())
        authToken = json.access_token
        return authToken
    }

    /**
     * Helper to convert a Map into a URL-encoded BodyPublisher
     */
    private static HttpRequest.BodyPublisher buildFormDataPublisher(Map<String, String> data) {
        HttpRequest.BodyPublishers.ofString(buildFormData(data));
    }

    private static String buildFormData(Map<String, String> data) {
        data.collect { String k, String v -> "${urlEncode(k)}=${urlEncode(v)}" }.join("&")
    }

    private static String urlEncode(String string) {
        URLEncoder.encode(string, StandardCharsets.UTF_8)
    }

}