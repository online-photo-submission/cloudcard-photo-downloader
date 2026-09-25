package com.cloudcard.photoDownloader

import com.cloudcard.photoDownloader.exception.CloudCardApiException
import org.springframework.test.util.ReflectionTestUtils
import software.amazon.awssdk.services.sts.model.Credentials
import spock.lang.Specification

import java.time.Instant

import static com.cloudcard.photoDownloader.CloudCardClient.*

/**
 * Policy only. RemotePhotoUtil is a global Groovy mock, so these run offline in milliseconds.
 * Global mocks only intercept calls from dynamically compiled Groovy, which CloudCardClient is --
 * including the calls inside the closures it hands to Retry. Anything that actually talks to a
 * server belongs in CloudCardClientIntegrationTests.
 */
class CloudCardClientSpec extends Specification {

    private static final String API_URL = "http://localhost:8082/api"
    private static final String AUTH_TOKEN = "test-auth-token"
    private static final String QUEUE_URL = "https://sqs.example/queue"

    PreProcessor preProcessor = Mock()
    TokenService tokenService = Mock()

    CloudCardClient client

    def setup() {
        GroovyMock(RemotePhotoUtil, global: true)

        tokenService.getAuthTokenValue() >> AUTH_TOKEN

        client = new CloudCardClient()
        ReflectionTestUtils.setField(client, "apiUrl", API_URL)
        ReflectionTestUtils.setField(client, "preProcessor", preProcessor)
        ReflectionTestUtils.setField(client, "tokenService", tokenService)

        // Keep the retry curve real but fast enough that a full exhaustion takes milliseconds.
        ReflectionTestUtils.setField(client, "maxAttempts", 3)
        ReflectionTestUtils.setField(client, "initialIntervalMillis", 1L)
        ReflectionTestUtils.setField(client, "maxIntervalMillis", 4L)

        // The Retry is built in @PostConstruct, which a plain unit test does not trigger.
        client.init()
    }

    /* *** RETRY CLASSIFICATION *** */

    def "a #status failure (permanent: #permanent) is attempted #attempts time(s)"() {
        when:
        client.fetch(READY_FOR_DOWNLOAD)

        then:
        attempts * RemotePhotoUtil.fetchPhotos(API_URL, AUTH_TOKEN, READY_FOR_DOWNLOAD) >> {
            throw new CloudCardApiException("failure", status, permanent)
        }

        and:
        CloudCardApiException e = thrown()
        e.status == status

        where:
        status | permanent || attempts
        503    | false     || 3
        502    | false     || 3
        403    | true      || 1
        401    | true      || 1
    }

    def "a transient failure followed by success returns the result"() {
        given:
        Photo photo = new Photo()

        when:
        List<Photo> photos = client.fetch(READY_FOR_DOWNLOAD)

        then:
        2 * RemotePhotoUtil.fetchPhotos(API_URL, AUTH_TOKEN, READY_FOR_DOWNLOAD) >>
            { throw new CloudCardApiException("boom", 503, false) } >> [photo]
        photos == [photo]
    }

    def "a permanent credential failure is not retried"() {
        when:
        client.fetchStsCredentials(QUEUE_URL)

        then:
        1 * RemotePhotoUtil.fetchStsCredentials(API_URL, AUTH_TOKEN, QUEUE_URL) >> {
            throw new CloudCardApiException("queue is shared", 403, true)
        }

        and:
        thrown(CloudCardApiException)
    }

    /* *** PHOTO BYTES *** */

    def "fetchBytes sets the bytes on the photo"() {
        given:
        byte[] expected = [1, 2, 3] as byte[]
        Photo photo = new Photo(externalURL: "https://example.test/photo.jpg")

        when:
        client.fetchBytes(photo)

        then:
        1 * RemotePhotoUtil.fetchBytes("https://example.test/photo.jpg") >> expected
        photo.bytes == expected
    }

    def "fetchBytes sets the bytes on the additional photo"() {
        given:
        byte[] expected = [4, 5, 6] as byte[]
        AdditionalPhoto additionalPhoto = new AdditionalPhoto(externalURL: "https://example.test/extra.jpg")

        when:
        client.fetchBytes(additionalPhoto)

        then:
        1 * RemotePhotoUtil.fetchBytes("https://example.test/extra.jpg") >> expected
        additionalPhoto.bytes == expected
    }

    def "fetchBytes propagates a permanent failure without setting bytes"() {
        given:
        Photo photo = new Photo(externalURL: "https://example.test/missing.jpg")

        when:
        client.fetchBytes(photo)

        then:
        1 * RemotePhotoUtil.fetchBytes(_) >> { throw new CloudCardApiException("not found", 404, true) }

        and:
        thrown(CloudCardApiException)
        photo.bytes == null
    }

    /* *** ORCHESTRATION *** */

    def "fetch concatenates results across statuses"() {
        given:
        Photo approved = new Photo()
        Photo ready = new Photo()

        when:
        List<Photo> photos = client.fetch([APPROVED, READY_FOR_DOWNLOAD] as String[])

        then:
        1 * RemotePhotoUtil.fetchPhotos(API_URL, AUTH_TOKEN, APPROVED) >> [approved]
        1 * RemotePhotoUtil.fetchPhotos(API_URL, AUTH_TOKEN, READY_FOR_DOWNLOAD) >> [ready]
        photos == [approved, ready]
    }

    def "fetchWithBytes pre-processes each photo and fetches its bytes"() {
        given:
        Photo raw = new Photo()
        Photo processed = new Photo(externalURL: "https://example.test/processed.jpg")
        byte[] bytes = [7, 8, 9] as byte[]

        when:
        client.fetchWithBytes([READY_FOR_DOWNLOAD] as String[])

        then:
        1 * RemotePhotoUtil.fetchPhotos(API_URL, AUTH_TOKEN, READY_FOR_DOWNLOAD) >> [raw]
        1 * preProcessor.process(raw) >> processed
        1 * RemotePhotoUtil.fetchBytes("https://example.test/processed.jpg") >> bytes
        processed.bytes == bytes
    }

    def "updateStatus returns the updated photo"() {
        given:
        Photo photo = new Photo(id: 42)
        Photo updated = new Photo(id: 42, status: DOWNLOADED)

        when:
        Photo result = client.updateStatus(photo, DOWNLOADED)

        then:
        1 * RemotePhotoUtil.updateStatus(API_URL, AUTH_TOKEN, photo, DOWNLOADED, null) >> updated
        result.is(updated)
    }

    def "fetchStsCredentials returns the brokered credentials"() {
        given:
        Credentials credentials = Credentials.builder()
            .accessKeyId("AKIA")
            .secretAccessKey("secret")
            .sessionToken("token")
            .expiration(Instant.now().plusSeconds(3600))
            .build()

        when:
        Credentials result = client.fetchStsCredentials(QUEUE_URL)

        then:
        1 * RemotePhotoUtil.fetchStsCredentials(API_URL, AUTH_TOKEN, QUEUE_URL) >> credentials
        result.is(credentials)
    }

    /* *** CONFIGURATION *** */

    def "with apiUrl '#apiUrl' and a token service configured: #tokenConfigured, isConfigured is #configured"() {
        given:
        ReflectionTestUtils.setField(client, "apiUrl", apiUrl)
        tokenService.isConfigured() >> tokenConfigured

        expect:
        client.isConfigured() == configured

        where:
        apiUrl  | tokenConfigured || configured
        API_URL | true            || true
        API_URL | false           || false
        ""      | true            || false
        null    | true            || false
    }
}