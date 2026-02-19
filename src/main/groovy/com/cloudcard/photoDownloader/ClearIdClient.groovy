package com.cloudcard.photoDownloader

import groovy.json.JsonSlurper
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

import java.nio.charset.StandardCharsets.*

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

@Component
@ConditionalOnProperty(value = "IntegrationStorageService.client", havingValue = "ClearIdClient")
class ClearIdClient implements IntegrationStorageClient {

    static final Logger log = LoggerFactory.getLogger(ClearIdClient.class)

    @Value('${ClearIdClient.apiUrl}')
    String apiUrl

    @Value('${ClearIdClient.stsUrl}')
    String stsUrl

    @Value('${ClearIdClient.accountId:customer}')
    String accountId

    @Value('${ClearIdClient.clientId:user}')
    String clientId

    @Value('${ClearIdClient.clientSecret:secret}')
    String clientSecret

    HttpClient client

    String authToken

    @PostConstruct
    void init() {
        throwIfBlank(apiUrl, "The ClearId API URL must be specified.")
        throwIfBlank(accountId, "The ClearId accountId must be specified")
        throwIfBlank(clientId, "The ClearId clientId must be specified")
        throwIfBlank(clientSecret, "The ClearId clientSecret must be specified")

        log.info("     ClearId API URL : $apiUrl")
        log.info("   ClearId accountId : $accountId")
        log.info("    ClearId clientId : $clientId")
        log.info("ClearId clientSecret : ${clientSecret.length() > 0 ? "......" : ""}")

        client = HttpClient.newBuilder().build()
    }

    @Override
    String getSystemName() {
        return "ClearId"
    }

    String getIdentity(String identifier) {
        //TODO I don't love this "withAuth" thing, but not sure how to do it better...
        // Can we create interceptors on the client?
        HttpRequest request = withAuth(
            HttpRequest.newBuilder()
                .uri(new URI("$apiUrl/accounts/$accountId/identities?ExternalId=$identifier"))
                .header("Accept", "application/json")
        ).build()

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() == 404) {
            throw new FailedPhotoFileException("ClearID identity not found for $identifier")
        }

        if (response.statusCode() != 200) {
            throw new Exception("Error finding ClearID identity for $identifier. Status: ${response.statusCode()}")
        }

        //TODO convert this to a type using an objectmapper or something
        def body = new JsonSlurper().parseText(response.body())

        if (!body.identities || body.identities.size() == 0) {
            throw new FailedPhotoFileException("ClearID identity not found for $identifier")
        }

        if (body.identities.size() > 1) {
            throw new FailedPhotoFileException("ClearID contains multiple users with ExternalID $identifier")
        }

        String identityId = body.identities[0].identityId
        return identityId
    }


    void putIdentityPicture(String identityId, byte[] photoBytes) {
        //TODO why are we doing this lol. use a proper formdata builder.
        String boundary = UUID.randomUUID().toString()
        byte[] separator = "--${boundary}\r\nContent-Disposition: form-data; name=\"picture\"; filename=\"picture.jpg\"\r\nContent-Type: image/jpeg\r\n\r\n".getBytes()
        byte[] endBoundary = "\r\n--${boundary}--\r\n".getBytes()

        //we may still need this one though.
        byte[] body = new byte[separator.length + photoBytes.length + endBoundary.length]
        System.arraycopy(separator, 0, body, 0, separator.length)
        System.arraycopy(fileData, 0, body, separator.length, photoBytes.length)
        System.arraycopy(endBoundary, 0, body, separator.length + photoBytes.length, endBoundary.length)


        //TODO I don't love this "withAuth" thing, but not sure how to do it better...
        // Can we create interceptors on the client?
        HttpRequest request = withAuth(
            HttpRequest.newBuilder()
                .uri(new URI("$apiUrl/accounts/$accountId/identities/$identityId/picture"))
                .header("Content-Type", "multipart/form-data; boundary=${boundary}")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
        ).build()

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) {
            throw new Exception("Error updating picture for ClearId identity $identityId. Status: ${response.statusCode()}")
        }

    }

//    TODO: Add resiliency to at least handle rate limiting.
    @Override
    void putPhoto(String identifier, byte[] photoBytes) {
        String identityId = getIdentity(identifier)

        putIdentityPicture(identityId, photoBytes)

        //TODO remove this when we're done testing.
        throw new FailedPhotoFileException("Upload to ClearId attempted. Remove hold to test again")
    }

    @Override
    void close() {

    }

    HttpRequest.Builder withAuth(HttpRequest.Builder builder) {
        builder.header("Authorization", "Bearer $authToken")
    }

    //TODO test this
    String authenticate() {
        Map<String, String> formData = [
            "client_id"    : clientId,
            "client_secret": clientSecret,
            "grant_type"   : "client_credentials"
        ]

        HttpRequest request = HttpRequest.newBuilder()
        //TODO figure out if the sts url is separate from the main api url.
            .uri(URI.create(stsUrl))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(buildFormDataPublisher(formData))
            .build()

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString())

        Object json = new JsonSlurper().parseText(response.body())

        // Output results
        System.out.println("Status Code: " + response.statusCode());
        System.out.println("Response: " + response.body());


        authToken = json.access_token
        return authToken
    }

    String getAuthToken() {
//        return authToken ?: authenticate()
        return "thisisauthtokenhaha"
    }

    /**
     * Helper to convert a Map into a URL-encoded BodyPublisher
     */
    private static HttpRequest.BodyPublisher buildFormDataPublisher(Map<String, String> data) {
        HttpRequest.BodyPublishers.ofString(buildFormData(data));
    }

    private static String buildFormData(Map<String, String> data) {
        data.collect { String k, String v ->
            "${URLEncoder.encode(k, UTF_8)}=${URLEncoder.encode(v, UTF_8)}"
        }.join("&")
    }

}