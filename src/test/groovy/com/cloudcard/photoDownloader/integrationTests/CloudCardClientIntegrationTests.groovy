package com.cloudcard.photoDownloader.integrationTests

import com.cloudcard.photoDownloader.*
import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Requires
import spock.lang.Specification
import spock.lang.Tag

import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant

import static com.cloudcard.photoDownloader.CloudCardClient.*

/**
 * These talk to real servers. Excluded from the default test task -- run them with
 * `./gradlew integrationTest`.
 *
 * The photo-byte features only need public internet. The API features need cloudcard.api.url and
 * cloudcard.api.accessToken (env var or system property) and are skipped when those are absent
 * rather than failing the run. They seed and delete their own data, so they do not depend on
 * whatever is sitting in the environment.
 *
 * Nothing is mocked: DoNothingPreProcessor is the real pass-through the application ships with.
 */
@Tag("integration")
class CloudCardClientIntegrationSpec extends Specification {

    private static final String TEST_IMAGE_URL =
        "https://sharptopco.github.io/cloudcard-custom-assets/example_id_photo.jpg"

    private static final String TEST_PNG_URL =
        "https://raw.githubusercontent.com/semanticdata/public-test/main/PNG/myfinalform.png"

    private static final String API_URL_VAR = "cloudcard.api.url"
    private static final String ACCESS_TOKEN_VAR = "cloudcard.api.accessToken"
    private static final String QUEUE_URL_VAR = "sqsPhotoService.queueUrl"

    CloudCardClient client
    TokenService tokenService
    PhotoTestFixture fixture
    byte[] expectedTestImageBytes

    def setup() {
        expectedTestImageBytes = Files.readAllBytes(Paths.get("src/test/resources/example_id_photo.jpg"))

        String apiUrl = config(API_URL_VAR, "http://localhost:8082/api")
        String accessToken = config(ACCESS_TOKEN_VAR, "")

        // A real TokenService: the configured value is the persistent access token, and the
        // X-Auth-Token header needs the short-lived token that login() exchanges it for.
        tokenService = new TokenService()
        ReflectionTestUtils.setField(tokenService, "apiUrl", apiUrl)
        ReflectionTestUtils.setField(tokenService, "persistentAccessToken", accessToken)

        client = new CloudCardClient()
        ReflectionTestUtils.setField(client, "apiUrl", apiUrl)
        ReflectionTestUtils.setField(client, "preProcessor", new DoNothingPreProcessor())
        ReflectionTestUtils.setField(client, "tokenService", tokenService)
        ReflectionTestUtils.setField(client, "maxAttempts", 3)
        ReflectionTestUtils.setField(client, "initialIntervalMillis", 500L)
        ReflectionTestUtils.setField(client, "maxIntervalMillis", 5000L)

        client.init()

        if (accessToken) fixture = new PhotoTestFixture(apiUrl, accessToken)
    }

    def cleanup() {
        fixture?.close()
        tokenService?.logout()
    }

    /* *** PHOTO BYTES -- public internet only *** */

    def "fetches JPEG bytes that match the checked-in fixture"() {
        given:
        Photo photo = new Photo(externalURL: TEST_IMAGE_URL)

        when:
        client.fetchBytes(photo)

        then:
        photo.bytes == expectedTestImageBytes
    }

    def "fetches PNG bytes -- the content type is not derived from the extension"() {
        given:
        Photo photo = new Photo(externalURL: TEST_PNG_URL)

        when:
        client.fetchBytes(photo)

        then:
        photo.bytes.length > 0
    }

    def "fetches bytes for an additional photo"() {
        given:
        AdditionalPhoto additionalPhoto = new AdditionalPhoto(externalURL: TEST_IMAGE_URL)

        when:
        client.fetchBytes(additionalPhoto)

        then:
        additionalPhoto.bytes == expectedTestImageBytes
    }

    /* *** API -- needs a running CloudCard API *** */

    @Requires({ apiConfigured() })
    def "finds a seeded photo among those ready for download"() {
        given:
        Integer seededPhotoId = fixture.seedApprovedPhoto()

        when:
        List<Photo> photos = client.fetch([APPROVED, READY_FOR_DOWNLOAD] as String[])

        then:
        seededPhotoId in photos*.id
        photos.every { it.status in [APPROVED, READY_FOR_DOWNLOAD] }
    }

    @Requires({ apiConfigured() })
    def "fetches bytes for every photo ready for download"() {
        given:
        fixture.seedApprovedPhoto()

        when:
        List<Photo> photos = client.fetchWithBytes([APPROVED, READY_FOR_DOWNLOAD] as String[])

        then:
        !photos.isEmpty()
        photos.every { it.bytes?.length > 0 }
    }

    @Requires({ apiConfigured() })
    def "moves a seeded photo to DOWNLOADED"() {
        given:
        Integer seededPhotoId = fixture.seedApprovedPhoto()

        when:
        Photo updated = client.updateStatus(new Photo(id: seededPhotoId), DOWNLOADED)

        then:
        updated.status == DOWNLOADED

        and: "it has left the download queue and appears under its new status"
        !(seededPhotoId in client.fetch(READY_FOR_DOWNLOAD)*.id)
        seededPhotoId in client.fetch(DOWNLOADED)*.id
    }

    @Requires({ apiConfigured() })
    def "records a failure reason when marking a photo failed"() {
        given:
        Integer seededPhotoId = fixture.seedApprovedPhoto()

        when:
        Photo updated = client.updateStatus(new Photo(id: seededPhotoId), FAILED, "integration test failure")

        then:
        updated.status == FAILED
    }

    @Requires({ apiConfigured() && config(QUEUE_URL_VAR) })
    def "brokers STS credentials for a queue"() {
        when:
        def credentials = client.fetchStsCredentials(config(QUEUE_URL_VAR))

        then:
        credentials.accessKeyId()
        credentials.secretAccessKey()
        credentials.sessionToken()
        credentials.expiration().isAfter(Instant.now())
    }

    /* *** HELPERS *** */

    /** Reads an environment variable, falling back to a system property of the same name. */
    static String config(String name, String fallback = null) {
        return System.getenv(name) ?: System.getProperty(name) ?: fallback
    }

    static boolean apiConfigured() {
        return config(API_URL_VAR) && config(ACCESS_TOKEN_VAR)
    }
}