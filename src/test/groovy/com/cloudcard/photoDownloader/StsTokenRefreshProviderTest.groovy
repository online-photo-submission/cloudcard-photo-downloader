package com.cloudcard.photoDownloader

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.services.sts.model.Credentials

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatThrownBy
import static org.mockito.Mockito.*

class StsTokenRefreshingProviderTest {

    private static final String QUEUE_URL = "https://sqs.ca-central-1.amazonaws.com/123456789012/status-queue"
    private static final Instant START = Instant.parse("2026-09-25T12:00:00Z")
    private static final Instant INITIAL_EXPIRY = START.plus(Duration.ofHours(1))
    private static final Instant REFRESHED_EXPIRY = START.plus(Duration.ofHours(2))

    CloudCardClient cloudCardClient
    TestClock clock
    StsTokenRefreshingProvider provider

    @BeforeEach
    void setup() {
        cloudCardClient = mock(CloudCardClient)
        clock = new TestClock(START)

        provider = new StsTokenRefreshingProvider(cloudCardClient, QUEUE_URL, clock)
    }

    @Test
    void fetchesAndMapsCredentialsOnFirstRequest() {
        when(cloudCardClient.fetchStsCredentials(QUEUE_URL))
            .thenReturn(credentials("first", INITIAL_EXPIRY))

        AwsSessionCredentials result =
            provider.resolveCredentials() as AwsSessionCredentials

        assertThat(result.accessKeyId()).isEqualTo("access-first")
        assertThat(result.secretAccessKey()).isEqualTo("secret-first")
        assertThat(result.sessionToken()).isEqualTo("session-first")

        verify(cloudCardClient).fetchStsCredentials(QUEUE_URL)
    }

    @Test
    void reusesCachedCredentialsBeforeRefreshWindow() {
        when(cloudCardClient.fetchStsCredentials(QUEUE_URL))
            .thenReturn(credentials("first", INITIAL_EXPIRY))

        AwsCredentials first = provider.resolveCredentials()

        clock.advance(Duration.ofMinutes(54).plusSeconds(59))

        AwsCredentials second = provider.resolveCredentials()

        assertThat(second).isSameAs(first)
        verify(cloudCardClient, times(1)).fetchStsCredentials(QUEUE_URL)
    }

    @Test
    void refreshesCredentialsInsideRefreshWindow() {
        when(cloudCardClient.fetchStsCredentials(QUEUE_URL))
            .thenReturn(
                credentials("first", INITIAL_EXPIRY),
                credentials("second", REFRESHED_EXPIRY)
            )

        provider.resolveCredentials()

        clock.advance(Duration.ofMinutes(55))

        AwsSessionCredentials result =
            provider.resolveCredentials() as AwsSessionCredentials

        assertThat(result.accessKeyId()).isEqualTo("access-second")
        verify(cloudCardClient, times(2)).fetchStsCredentials(QUEUE_URL)
    }

    @Test
    void usesCachedCredentialsWhenRefreshFailsBeforeExpiry() {
        RuntimeException failure =
            new RuntimeException("CloudCard API unavailable")

        when(cloudCardClient.fetchStsCredentials(QUEUE_URL))
            .thenReturn(credentials("first", INITIAL_EXPIRY))
            .thenThrow(failure)

        AwsCredentials first = provider.resolveCredentials()

        clock.advance(Duration.ofMinutes(55))

        assertThat(provider.resolveCredentials()).isSameAs(first)
        verify(cloudCardClient, times(2)).fetchStsCredentials(QUEUE_URL)
    }

    @Test
    void throwsRefreshFailureAfterCachedCredentialsExpire() {
        RuntimeException failure =
            new RuntimeException("CloudCard API unavailable")

        when(cloudCardClient.fetchStsCredentials(QUEUE_URL))
            .thenReturn(credentials("first", INITIAL_EXPIRY))
            .thenThrow(failure)

        provider.resolveCredentials()

        clock.advance(Duration.ofMinutes(55))
        provider.resolveCredentials() // Still-valid cached fallback

        clock.advance(Duration.ofMinutes(5))

        assertThatThrownBy {
            provider.resolveCredentials()
        }.isSameAs(failure)

        verify(cloudCardClient, times(3)).fetchStsCredentials(QUEUE_URL)
    }

    @Test
    void throwsWhenTheFirstFetchFails() {
        RuntimeException failure = new RuntimeException("CloudCard API unavailable")
        when(cloudCardClient.fetchStsCredentials(QUEUE_URL)).thenThrow(failure)

        assertThatThrownBy { provider.resolveCredentials() }.isSameAs(failure)
    }

    @Test
    void rejectsIncompleteCredentials() {
        when(cloudCardClient.fetchStsCredentials(QUEUE_URL)).thenReturn(
            Credentials.builder()
                .accessKeyId("access")
                .secretAccessKey("secret")
                .expiration(INITIAL_EXPIRY)
                .build())

        assertThatThrownBy { provider.resolveCredentials() }
            .isInstanceOf(IllegalStateException)
    }

    @Test
    void rejectsCredentialsThatAreAlreadyExpired() {
        when(cloudCardClient.fetchStsCredentials(QUEUE_URL))
            .thenReturn(credentials("stale", START.minus(Duration.ofMinutes(1))))

        assertThatThrownBy { provider.resolveCredentials() }
            .isInstanceOf(IllegalStateException)
            .hasMessageContaining("clock")
    }

    private static Credentials credentials(String name, Instant expiration) {
        Credentials.builder()
                   .accessKeyId("access-${name}")
                   .secretAccessKey("secret-${name}")
                   .sessionToken("session-${name}")
                   .expiration(expiration)
                   .build()
    }

    private static final class TestClock extends Clock {
        private Instant current

        TestClock(Instant start) { current = start }

        @Override
        Instant instant() { current }

        @Override
        ZoneId getZone() { ZoneOffset.UTC }

        @Override
        Clock withZone(ZoneId zone) { new TestClock(current) }

        void advance(Duration duration) { current = current.plus(duration) }
    }
}
