package com.cloudcard.photoDownloader

import com.cloudcard.photoDownloader.exception.CloudCardApiException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kong.unirest.core.HttpResponse
import kong.unirest.core.RawResponse
import kong.unirest.core.Unirest
import kong.unirest.core.UnirestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sts.model.Credentials

import java.nio.charset.StandardCharsets
import java.time.Instant

/**
 * Stateless RemotePhoto API adapter: URLs, headers, serialisation. No retry policy, no token lifecycle.
 *
 * Every method throws CloudCardApiException on a non-success status or a network failure, so that
 * callers -- and the Retry in CloudCardClient -- can tell a transient failure from a permanent one
 * without knowing anything about Unirest. Nothing here returns null to signal failure.
 *
 * Static because RemoteConfigInitializer runs before the Spring context exists and cannot inject
 * anything. CloudCardClient is the bean that wraps these for everything that runs after startup.
 */
class RemotePhotoHttpAdapter {

    private static final Logger log = LoggerFactory.getLogger(RemotePhotoHttpAdapter.class)
    private static final ObjectMapper objectMapper = new ObjectMapper()

    /** Statuses that will never succeed on a retry: auth, authorization, and bad requests. */
    private static final List<Integer> PERMANENT_STATUSES = [400, 401, 403, 422]

    /* *** AUTHENTICATION *** */

    static AuthenticationToken login(String apiUrl, String persistentAccessToken) throws Exception {
        String url = "${apiUrl}/authenticationTokens"

        HttpResponse<String> response = send("logging in") {
            Unirest.post(url)
                .headers(standardHeaders())
                .body(objectMapper.writeValueAsString([persistentAccessToken: persistentAccessToken]))
                .asString()
        }

        requireStatus(response, 200, "logging in")

        return objectMapper.readValue(response.body, new TypeReference<AuthenticationToken>() {})
    }

    /**
     * Never throws. A failed logout is not worth failing a cycle over, and the token expires anyway.
     */
    static void logout(String apiUrl, String authToken) {
        if (!authToken) return

        try {
            String url = "${apiUrl}/people/me/logout"

            HttpResponse<String> response = send("logging out") {
                Unirest.post(url)
                    .headers(standardHeaders(authToken))
                    .body(objectMapper.writeValueAsString([authenticationToken: authToken]))
                    .asString()
            }

            if (response.status != 204) {
                log.warn("Status ${response.status} returned from the CloudCard API when logging out.")
            }

        } catch (Exception e) {
            log.warn("Exception thrown while logging out of the CloudCard API.", e)
        }
    }

    /* *** CONFIGURATION *** */

    static Integration getRemoteConfig(String apiUrl, String integrationName, String authToken) throws Exception {
        if (!integrationName) {
            throw new IllegalArgumentException("cloudcard.integration.name must be set when using remote configs.")
        }

        String url = "${apiUrl}/integrations/${integrationName}?findBy=name"
        String context = "retrieving the integration '${integrationName}'"

        HttpResponse<String> response = send(context) {
            Unirest.get(url)
                .headers(standardHeaders(authToken))
                .asString()
        }

        requireStatus(response, 200, context)

        return objectMapper.readValue(response.body, new TypeReference<Integration>() {})
    }

    /* *** PHOTOS *** */

    static List<Photo> fetchPhotos(String apiUrl, String authToken, String status) throws Exception {
        String url = "${apiUrl}/trucredential/${authToken}/photos" +
            "?status=${status}&base64EncodedImage=false&max=1000&additionalPhotos=true"
        String context = "retrieving the photo list for status ${status}"

        HttpResponse<String> response = send(context) {
            Unirest.get(url)
                .headers(standardHeaders(authToken))
                .asString()
        }

        requireStatus(response, 200, context)

        return objectMapper.readValue(response.body, new TypeReference<List<Photo>>() {})
    }

    static Photo updateStatus(String apiUrl, String authToken, Photo photo, String status, String message) throws Exception {
        String url = "${apiUrl}/photos/${photo.id}"
        String context = "updating the status of photo ${photo.id} to ${status}"

        if (message) {
            String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8)
            if (status == CloudCardClient.ON_HOLD) url += "?onHoldReason=${encoded}"
            if (status == CloudCardClient.FAILED) url += "?failedReason=${encoded}"
        }

        HttpResponse<String> response = send(context) {
            Unirest.put(url)
                .headers(standardHeaders(authToken))
                .body(objectMapper.writeValueAsString([status: status]))
                .asString()
        }

        requireStatus(response, 200, context)

        return objectMapper.readValue(response.body, new TypeReference<Photo>() {})
    }

    /* *** CREDENTIAL BROKERING *** */

    static Credentials fetchStsCredentials(String apiUrl, String authToken, String queueUrl) throws Exception {
        String url = "${apiUrl}/status-queues/credentials"
        String context = "brokering credentials for ${queueUrl}"

        HttpResponse<String> response = send(context) {
            Unirest.post(url)
                .headers(standardHeaders(authToken))
                .body(objectMapper.writeValueAsString([queueUrl: queueUrl]))
                .asString()
        }

        requireStatus(response, 200, context)

        Map<String, Object> body = objectMapper.readValue(response.body, new TypeReference<Map<String, Object>>() {})

        return Credentials.builder()
            .accessKeyId(body.accessKeyId as String)
            .secretAccessKey(body.secretAccessKey as String)
            .sessionToken(body.sessionToken as String)
            .expiration(body.expiration ? Instant.parse(body.expiration as String) : null)
            .build()
    }

    /* *** PHOTO BYTES *** */

    /**
     * Fetches the image itself. The URL points at wherever the photo is hosted, not at the
     * CloudCard API, so no auth headers are sent.
     */
    static byte[] fetchBytes(String externalURL) throws Exception {
        if (!externalURL) {
            throw new IllegalArgumentException("Cannot fetch photo bytes without an external URL.")
        }

        HttpResponse<byte[]> response = send("retrieving photo bytes from ${externalURL}") {
            Unirest.get(externalURL)
                .asObject(RawResponse.&getContentAsBytes)
        }

        // Not requireStatus: this is the photo host, not the CloudCard API, and the message should say so.
        if (response.status != 200) {
            throw new CloudCardApiException(
                "Photo host returned ${response.status} when retrieving photo bytes from ${externalURL}.",
                response.status,
                isPermanent(response.status))
        }

        byte[] bytes = response.body

        if (!bytes || bytes.length == 0) {
            throw new CloudCardApiException(
                "Empty response body returned when retrieving photo bytes from ${externalURL}.", 200, false)
        }

        return bytes
    }

    /* *** PRIVATE HELPERS *** */

    /**
     * Runs a Unirest request and translates transport failures -- refused connections, timeouts,
     * resets -- into a transient CloudCardApiException with status 0, so no caller ever sees Unirest types.
     */
    private static <T> HttpResponse<T> send(String context, Closure<HttpResponse<T>> request) {
        try {
            return request()
        } catch (UnirestException e) {
            throw new CloudCardApiException("Network failure while ${context}.", 0, false, e)
        }
    }

    private static void requireStatus(HttpResponse<String> response, int expected, String context) {
        if (response.status == expected) return

        throw new CloudCardApiException(
            "CloudCard API returned ${response.status} when ${context}.",
            response.status,
            isPermanent(response.status))
    }

    private static boolean isPermanent(int status) {
        return PERMANENT_STATUSES.contains(status)
    }

    private static Map<String, String> standardHeaders(String authToken = null) {
        Map<String, String> headers = [
            "accept"      : "application/json",
            "Content-Type": "application/json"
        ]

        if (authToken) headers.put("X-Auth-Token", authToken)

        return headers
    }
}