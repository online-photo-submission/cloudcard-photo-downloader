package com.cloudcard.photoDownloader.integrationTests

import com.cloudcard.photoDownloader.CloudCardClient
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import kong.unirest.core.HttpResponse
import kong.unirest.core.Unirest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Paths

/**
 * Seeds data for the integration tests so they do not depend on whatever happens to be sitting in
 * a dev environment.
 *
 *   office PAT -> office token -> create person -> person's PAT -> person's token -> submit photo
 *   -> approve photo (as the office user)
 *
 * Every person it creates is recorded and deleted by cleanUp(), so a run leaves nothing behind.
 * Test-only. Never wire this into the application.
 */
class PhotoTestFixture {

    private static final Logger log = LoggerFactory.getLogger(PhotoTestFixture.class)

    private static final String DEFAULT_IMAGE = "src/test/resources/example_id_photo.jpg"

    /** apiUrl already ends in /api, where the plural resource paths live. */
    private final String apiUrl
    private final String officeToken

    private final List<String> createdPersonIds = []

    PhotoTestFixture(String apiUrl, String officePat) {
        this.apiUrl = apiUrl
        this.officeToken = authenticate(officePat)
    }

    /**
     * Creates a cardholder, submits a photo for them, and approves it.
     *
     * @return the id of the approved photo
     */
    Integer seedApprovedPhoto(String imagePath = DEFAULT_IMAGE) {
        byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath))
        String encodedImage = Base64.encoder.encodeToString(imageBytes)

        String personId = createPerson()
        String personToken = authenticate(fetchPersonPat(personId))

        Integer photoId
        try {
            photoId = submitPhoto(personToken, personId, encodedImage, imageBytes.length)
        } finally {
            logout(personToken)
        }

        approvePhoto(photoId)

        log.info("Seeded person ${personId} with approved photo ${photoId}.")

        return photoId
    }

    /** Deletes everything this fixture created. Safe to call more than once. */
    void cleanUp() {
        createdPersonIds.reverse().each { deletePerson(it) }
        createdPersonIds.clear()
    }

    void close() {
        cleanUp()
        logout(officeToken)
    }

    /* *** SETUP STEPS *** */

    private String createPerson() {
        String suffix = UUID.randomUUID().toString().replace("-", "").take(12)

        HttpResponse<String> response = Unirest.post("${apiUrl}/people")
            .headers(headers(officeToken))
            .body(new JsonBuilder([
                email                  : "downloader-test+${suffix}@cloudcard.us",
                identifier             : "downloader-text${suffix}",
                additionalPhotoRequired: false
            ]).toString())
            .asString()

        requireStatus(response, 201, "creating a test person")

        String personId = idFromLocation(response)
        createdPersonIds << personId

        return personId
    }

    private String fetchPersonPat(String personId) {
        HttpResponse<String> response = Unirest.get("${apiUrl}/people/${personId}/with-links")
            .headers(headers(officeToken))
            .asString()

        requireStatus(response, 200, "fetching the PAT for person ${personId}")

        return new JsonSlurper().parseText(response.body).links.login.tokenize("=").last()
    }

    private Integer submitPhoto(String personToken, String personId, String encodedImage, long imageSize) {
        HttpResponse<String> response = Unirest.post("${apiUrl}/photos")
            .headers(headers(personToken))
            .queryString("imageSize", imageSize)
            .body(new JsonBuilder([
                person      : [id: personId],
                encodedImage: encodedImage
            ]).toString())
            .asString()

        requireStatus(response, 201, "submitting a photo for person ${personId}")

        def body = new JsonSlurper().parseText(response.body)

        if (!body?.id) {
            throw new IllegalStateException("No photo id in the response. Body: ${response.body}")
        }

        return body.id as Integer
    }

    /** Same endpoint the downloader uses for status changes, called as the office user. */
    private void approvePhoto(Integer photoId) {
        HttpResponse<String> response = Unirest.put("${apiUrl}/photos/${photoId}")
            .headers(headers(officeToken))
            .body(new JsonBuilder([status: CloudCardClient.APPROVED]).toString())
            .asString()

        requireStatus(response, 200, "approving photo ${photoId}")
    }

    private void deletePerson(String personId) {
        try {
            HttpResponse<String> response = Unirest.delete("${apiUrl}/people/${personId}")
                .headers(headers(officeToken))
                .asString()

            if (!(response.status in [200, 204])) {
                log.warn("Status ${response.status} returned when deleting test person ${personId}.")
            }

        } catch (Exception e) {
            log.warn("Failed to delete test person ${personId}.", e)
        }
    }

    /* *** AUTH *** */

    private String authenticate(String persistentAccessToken) {
        HttpResponse<String> response = Unirest.post("${apiUrl}/authenticationTokens")
            .headers(headers(null))
            .body(new JsonBuilder([persistentAccessToken: persistentAccessToken]).toString())
            .asString()

        requireStatus(response, 200, "authenticating")

        return new JsonSlurper().parseText(response.body).tokenValue
    }

    private void logout(String token) {
        if (!token) return

        try {
            Unirest.post("${apiUrl}/people/me/logout")
                .headers(headers(token))
                .body(new JsonBuilder([authenticationToken: token]).toString())
                .asString()
        } catch (Exception e) {
            log.warn("Failed to log out a fixture token.", e)
        }
    }

    /* *** HELPERS *** */

    private static String idFromLocation(HttpResponse<String> response) {
        String location = response.headers.getFirst("Location")

        if (!location) {
            throw new IllegalStateException("No Location header returned. Body: ${response.body}")
        }

        return location.tokenize("/").last()
    }

    private static Map<String, String> headers(String authToken) {
        Map<String, String> headers = [
            "accept"      : "application/json",
            "Content-Type": "application/json"
        ]

        if (authToken) headers.put("X-Auth-Token", authToken)

        return headers
    }

    private static void requireStatus(HttpResponse<String> response, int expected, String context) {
        if (response.status == expected) return

        throw new IllegalStateException(
            "Fixture failed while ${context}: expected ${expected}, got ${response.status}. Body: ${response.body}")
    }
}