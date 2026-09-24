package com.cloudcard.photoDownloader.integrationTests

import com.cloudcard.photoDownloader.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.test.util.ReflectionTestUtils

import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant

import static com.cloudcard.photoDownloader.CloudCardClient.*
import static org.assertj.core.api.Assertions.assertThat
import static org.junit.jupiter.api.Assumptions.assumeTrue
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when

/**
 * These talk to real servers. Excluded from the default test task -- run them with
 * `./gradlew integrationTest`.
 *
 * The photo-byte tests only need public internet. The API tests need cloudcard.api.url and
 * cloudcard.api.accessToken (env var or system property) and are skipped when those are absent
 * rather than failing the run. They seed and delete their own data, so they do not depend on
 * whatever is sitting in the environment.
 */
@Tag("integration")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class CloudCardClientIntegrationTests {

    private static final String TEST_IMAGE_URL =
        "https://sharptopco.github.io/cloudcard-custom-assets/example_id_photo.jpg"

    private static final String TEST_PNG_URL =
        "https://raw.githubusercontent.com/semanticdata/public-test/main/PNG/myfinalform.png"

    private static final String API_URL_VAR = "cloudcard.api.url"
    private static final String ACCESS_TOKEN_VAR = "cloudcard.api.accessToken"
    private static final String QUEUE_URL_VAR = "sqsPhotoService.queueUrl"

    @Mock
    PreProcessor mockPreProcessor

    CloudCardClient client
    TokenService tokenService
    PhotoTestFixture fixture
    byte[] expectedTestImageBytes

    @BeforeEach
    void setup() {
        expectedTestImageBytes = Files.readAllBytes(Paths.get("src/test/resources/example_id_photo.jpg"))

        when(mockPreProcessor.process(any(Photo.class))).thenAnswer { it.getArgument(0) }

        String apiUrl = config(API_URL_VAR, "http://localhost:8082/api")
        String accessToken = config(ACCESS_TOKEN_VAR, "")

        // A real TokenService: the configured value is the persistent access token, and the
        // X-Auth-Token header needs the short-lived token that login() exchanges it for.
        tokenService = new TokenService()
        ReflectionTestUtils.setField(tokenService, "apiUrl", apiUrl)
        ReflectionTestUtils.setField(tokenService, "persistentAccessToken", accessToken)

        client = new CloudCardClient()
        ReflectionTestUtils.setField(client, "apiUrl", apiUrl)
        ReflectionTestUtils.setField(client, "preProcessor", mockPreProcessor)
        ReflectionTestUtils.setField(client, "tokenService", tokenService)
        ReflectionTestUtils.setField(client, "maxAttempts", 3)
        ReflectionTestUtils.setField(client, "initialIntervalMillis", 500L)
        ReflectionTestUtils.setField(client, "maxIntervalMillis", 5000L)

        client.init()

        if (accessToken) fixture = new PhotoTestFixture(apiUrl, accessToken)
    }

    @AfterEach
    void teardown() {
        fixture?.close()
        tokenService?.logout()
    }

    /* *** PHOTO BYTES -- public internet only *** */

    @Test
    @DisplayName("fetches JPEG bytes and matches the checked-in fixture")
    void fetchesJpegBytesForAPhoto() {
        Photo photo = new Photo(externalURL: TEST_IMAGE_URL)

        client.fetchBytes(photo)

        assertThat(photo.bytes).isEqualTo(expectedTestImageBytes)
    }

    @Test
    @DisplayName("fetches PNG bytes -- the content type is not derived from the extension")
    void fetchesPngBytesForAPhoto() {
        Photo photo = new Photo(externalURL: TEST_PNG_URL)

        client.fetchBytes(photo)

        assertThat(photo.bytes.length).isGreaterThan(0)
    }

    @Test
    @DisplayName("fetches bytes for an additional photo")
    void fetchesBytesForAnAdditionalPhoto() {
        AdditionalPhoto additionalPhoto = new AdditionalPhoto(externalURL: TEST_IMAGE_URL)

        client.fetchBytes(additionalPhoto)

        assertThat(additionalPhoto.bytes).isEqualTo(expectedTestImageBytes)
    }

    /* *** API -- needs a running CloudCard API *** */

    @Test
    @DisplayName("finds a seeded photo among those ready for download")
    void fetchesReadyForDownload() {
        assumeApiAvailable()

        Integer seededPhotoId = fixture.seedApprovedPhoto()

        List<Photo> photos = client.fetch([APPROVED, READY_FOR_DOWNLOAD] as String[])

        assertThat(photos*.id).contains(seededPhotoId)
        photos.each { assertThat([APPROVED, READY_FOR_DOWNLOAD]).contains(it.status) }
    }

    @Test
    @DisplayName("fetches bytes for every photo ready for download")
    void fetchWithBytesPopulatesEveryPhoto() {
        assumeApiAvailable()

        fixture.seedApprovedPhoto()

        List<Photo> photos = client.fetchWithBytes([APPROVED, READY_FOR_DOWNLOAD] as String[])

        assertThat(photos).isNotEmpty()
        photos.each { assertThat(it.bytes.length).isGreaterThan(0) }
    }

    @Test
    @DisplayName("moves a seeded photo to DOWNLOADED")
    void updatesPhotoStatus() {
        assumeApiAvailable()

        Integer seededPhotoId = fixture.seedApprovedPhoto()

        Photo updated = client.updateStatus(new Photo(id: seededPhotoId), DOWNLOADED)

        assertThat(updated).isNotNull()
        assertThat(updated.status).isEqualTo(DOWNLOADED)

        // The photo has left the download queue and can be found under its new status.
        assertThat(client.fetch(READY_FOR_DOWNLOAD)*.id).doesNotContain(seededPhotoId)
        assertThat(client.fetch(DOWNLOADED)*.id).contains(seededPhotoId)
    }

    @Test
    @DisplayName("records a failure reason when marking a photo failed")
    void updatesPhotoStatusWithAMessage() {
        assumeApiAvailable()

        Integer seededPhotoId = fixture.seedApprovedPhoto()

        Photo updated = client.updateStatus(new Photo(id: seededPhotoId), FAILED, "integration test failure")

        assertThat(updated).isNotNull()
        assertThat(updated.status).isEqualTo(FAILED)
    }

    @Test
    @DisplayName("brokers STS credentials for a queue")
    void brokersStsCredentials() {
        assumeApiAvailable()

        String queueUrl = config(QUEUE_URL_VAR)
        assumeTrue(queueUrl as boolean, "${QUEUE_URL_VAR} not set")

        def credentials = client.fetchStsCredentials(queueUrl)

        assertThat(credentials.accessKeyId()).isNotBlank()
        assertThat(credentials.secretAccessKey()).isNotBlank()
        assertThat(credentials.sessionToken()).isNotBlank()
        assertThat(credentials.expiration()).isAfter(Instant.now())
    }

    /* *** HELPERS *** */

    /** Reads an environment variable, falling back to a system property of the same name. */
    private static String config(String name, String fallback = null) {
        return System.getenv(name) ?: System.getProperty(name) ?: fallback
    }

    private static void assumeApiAvailable() {
        assumeTrue(config(API_URL_VAR) as boolean, "${API_URL_VAR} not set")
        assumeTrue(config(ACCESS_TOKEN_VAR) as boolean, "${ACCESS_TOKEN_VAR} not set")
    }
}