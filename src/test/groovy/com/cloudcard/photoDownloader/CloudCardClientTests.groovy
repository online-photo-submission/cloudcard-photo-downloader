package com.cloudcard.photoDownloader

import com.cloudcard.photoDownloader.exception.CloudCardApiException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.MockedStatic
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.springframework.test.util.ReflectionTestUtils
import software.amazon.awssdk.services.sts.model.Credentials

import java.time.Instant

import static com.cloudcard.photoDownloader.CloudCardClient.*
import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatThrownBy
import static org.mockito.ArgumentMatchers.*
import static org.mockito.Mockito.*

/**
 * Policy only. Every HTTP call is behind a static on RemotePhotoUtil, which is mocked here, so
 * these run offline in milliseconds. Anything that actually talks to a server belongs in
 * CloudCardClientIntegrationTests.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CloudCardClientTests {

    private static final String API_URL = "http://localhost:8082/api"
    private static final String AUTH_TOKEN = "test-auth-token"

    @Mock
    PreProcessor mockPreProcessor

    @Mock
    TokenService mockTokenService

    CloudCardClient client
    MockedStatic<RemotePhotoUtil> remotePhotoUtil

    @BeforeEach
    void setup() {
        remotePhotoUtil = mockStatic(RemotePhotoUtil.class)

        when(mockTokenService.authTokenValue).thenReturn(AUTH_TOKEN)

        client = new CloudCardClient()
        ReflectionTestUtils.setField(client, "apiUrl", API_URL)
        ReflectionTestUtils.setField(client, "preProcessor", mockPreProcessor)
        ReflectionTestUtils.setField(client, "tokenService", mockTokenService)

        // Keep the retry curve real but fast enough that a full exhaustion takes milliseconds.
        ReflectionTestUtils.setField(client, "maxAttempts", 3)
        ReflectionTestUtils.setField(client, "initialIntervalMillis", 1L)
        ReflectionTestUtils.setField(client, "maxIntervalMillis", 4L)

        // The Retry is built in @PostConstruct, which a plain unit test does not trigger.
        client.init()
    }

    @AfterEach
    void teardown() {
        remotePhotoUtil.close()
    }

    /* *** RETRY CLASSIFICATION *** */

    @Test
    void transientFailuresAreRetriedUpToMaxAttempts() {
        remotePhotoUtil.when { RemotePhotoUtil.fetchPhotos(anyString(), anyString(), anyString()) }
            .thenThrow(new CloudCardApiException("boom", 503, false))

        assertThatThrownBy { client.fetch(READY_FOR_DOWNLOAD) }
            .isInstanceOf(CloudCardApiException.class)

        remotePhotoUtil.verify({ RemotePhotoUtil.fetchPhotos(anyString(), anyString(), anyString()) }, times(3))
    }

    @Test
    void permanentFailuresAreNotRetried() {
        remotePhotoUtil.when { RemotePhotoUtil.fetchPhotos(anyString(), anyString(), anyString()) }
            .thenThrow(new CloudCardApiException("forbidden", 403, true))

        assertThatThrownBy { client.fetch(READY_FOR_DOWNLOAD) }
            .isInstanceOf(CloudCardApiException.class)
            .hasFieldOrPropertyWithValue("status", 403)

        remotePhotoUtil.verify({ RemotePhotoUtil.fetchPhotos(anyString(), anyString(), anyString()) }, times(1))
    }

    @Test
    void aTransientFailureFollowedBySuccessReturnsTheResult() {
        Photo photo = new Photo()

        remotePhotoUtil.when { RemotePhotoUtil.fetchPhotos(anyString(), anyString(), anyString()) }
            .thenThrow(new CloudCardApiException("boom", 503, false))
            .thenReturn([photo])

        List<Photo> photos = client.fetch(READY_FOR_DOWNLOAD)

        assertThat(photos).containsExactly(photo)
        remotePhotoUtil.verify({ RemotePhotoUtil.fetchPhotos(anyString(), anyString(), anyString()) }, times(2))
    }

    @Test
    void permanentCredentialFailuresAreNotRetried() {
        remotePhotoUtil.when { RemotePhotoUtil.fetchStsCredentials(anyString(), anyString(), anyString()) }
            .thenThrow(new CloudCardApiException("queue is shared", 403, true))

        assertThatThrownBy { client.fetchStsCredentials("https://sqs.example/queue") }
            .isInstanceOf(CloudCardApiException.class)

        remotePhotoUtil.verify({ RemotePhotoUtil.fetchStsCredentials(anyString(), anyString(), anyString()) }, times(1))
    }

    /* *** PHOTO BYTES *** */

    @Test
    void fetchBytesSetsTheBytesOnThePhoto() {
        byte[] expected = [1, 2, 3] as byte[]
        Photo photo = new Photo(externalURL: "https://example.test/photo.jpg")

        remotePhotoUtil.when { RemotePhotoUtil.fetchBytes("https://example.test/photo.jpg") }.thenReturn(expected)

        client.fetchBytes(photo)

        assertThat(photo.bytes).isEqualTo(expected)
    }

    @Test
    void fetchBytesSetsTheBytesOnTheAdditionalPhoto() {
        byte[] expected = [4, 5, 6] as byte[]
        AdditionalPhoto additionalPhoto = new AdditionalPhoto(externalURL: "https://example.test/extra.jpg")

        remotePhotoUtil.when { RemotePhotoUtil.fetchBytes("https://example.test/extra.jpg") }.thenReturn(expected)

        client.fetchBytes(additionalPhoto)

        assertThat(additionalPhoto.bytes).isEqualTo(expected)
    }

    @Test
    void fetchBytesPropagatesAPermanentFailureWithoutSettingBytes() {
        Photo photo = new Photo(externalURL: "https://example.test/missing.jpg")

        remotePhotoUtil.when { RemotePhotoUtil.fetchBytes(anyString()) }
            .thenThrow(new CloudCardApiException("not found", 404, true))

        assertThatThrownBy { client.fetchBytes(photo) }.isInstanceOf(CloudCardApiException.class)

        assertThat(photo.bytes).isNull()
    }

    /* *** ORCHESTRATION *** */

    @Test
    void fetchConcatenatesResultsAcrossStatuses() {
        Photo approved = new Photo()
        Photo ready = new Photo()

        remotePhotoUtil.when { RemotePhotoUtil.fetchPhotos(anyString(), anyString(), eq(APPROVED)) }.thenReturn([approved])
        remotePhotoUtil.when { RemotePhotoUtil.fetchPhotos(anyString(), anyString(), eq(READY_FOR_DOWNLOAD)) }.thenReturn([ready])

        List<Photo> photos = client.fetch([APPROVED, READY_FOR_DOWNLOAD] as String[])

        assertThat(photos).containsExactly(approved, ready)
    }

    @Test
    void fetchWithBytesPreProcessesAndFetchesBytesForEachPhoto() {
        Photo raw = new Photo()
        Photo processed = new Photo(externalURL: "https://example.test/processed.jpg")
        byte[] bytes = [7, 8, 9] as byte[]

        remotePhotoUtil.when { RemotePhotoUtil.fetchPhotos(anyString(), anyString(), anyString()) }.thenReturn([raw])
        remotePhotoUtil.when { RemotePhotoUtil.fetchBytes("https://example.test/processed.jpg") }.thenReturn(bytes)
        when(mockPreProcessor.process(any(Photo.class))).thenReturn(processed)

        client.fetchWithBytes([READY_FOR_DOWNLOAD] as String[])

        verify(mockPreProcessor, times(1)).process(raw)
        assertThat(processed.bytes).isEqualTo(bytes)
    }

    @Test
    void updateStatusReturnsTheUpdatedPhoto() {
        Photo photo = new Photo(id: 42)
        Photo updated = new Photo(id: 42, status: DOWNLOADED)

        remotePhotoUtil.when { RemotePhotoUtil.updateStatus(anyString(), anyString(), any(Photo.class), anyString(), any()) }
            .thenReturn(updated)

        assertThat(client.updateStatus(photo, DOWNLOADED)).isSameAs(updated)
    }

    @Test
    void fetchStsCredentialsReturnsTheBrokeredCredentials() {
        Credentials credentials = Credentials.builder()
            .accessKeyId("AKIA")
            .secretAccessKey("secret")
            .sessionToken("token")
            .expiration(Instant.now().plusSeconds(3600))
            .build()

        remotePhotoUtil.when { RemotePhotoUtil.fetchStsCredentials(anyString(), anyString(), anyString()) }
            .thenReturn(credentials)

        assertThat(client.fetchStsCredentials("https://sqs.example/queue")).isSameAs(credentials)
    }

    /* *** CONFIGURATION *** */

    @Test
    void isConfiguredRequiresBothAnApiUrlAndAConfiguredTokenService() {
        when(mockTokenService.isConfigured()).thenReturn(true)
        assertThat(client.isConfigured()).isTrue()

        when(mockTokenService.isConfigured()).thenReturn(false)
        assertThat(client.isConfigured()).isFalse()

        when(mockTokenService.isConfigured()).thenReturn(true)
        ReflectionTestUtils.setField(client, "apiUrl", "")
        assertThat(client.isConfigured()).isFalse()
    }

    @Test
    void closeLogsOutOfTheTokenService() {
        client.close()
        verify(mockTokenService, times(1)).logout()
    }
}
