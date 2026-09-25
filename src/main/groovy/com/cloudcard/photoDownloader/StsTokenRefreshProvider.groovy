package com.cloudcard.photoDownloader

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.services.sts.model.Credentials

import java.time.Clock
import java.time.Instant

class StsTokenRefreshProvider implements AwsCredentialsProvider {

    private static final Logger log = LoggerFactory.getLogger(StsTokenRefreshProvider)

    private static final int REFRESH_BUFFER_SECONDS = 300

    private final CloudCardClient cloudCardClient
    private final String queueUrl
    private final Clock clock

    private volatile StsSession currentSession

    StsTokenRefreshProvider(CloudCardClient cloudCardClient, String queueUrl, Clock clock = Clock.systemUTC()) {
        this.cloudCardClient = cloudCardClient
        this.queueUrl = queueUrl
        this.clock = clock
    }

    @Override
    synchronized AwsCredentials resolveCredentials() {
        StsSession session = currentSession

        if (session && clock.instant().isBefore(session.refreshAt)) return session.credentials

        try {
            currentSession = fetchCredentials()
            return currentSession.credentials
        } catch (Exception e) {
            session = currentSession

            if (session && clock.instant().isBefore(session.expiresAt)) {
                log.warn("Unable to refresh SQS credentials; using the current session until {}.", session.expiresAt)
                return session.credentials
            }
            throw e
        }
    }

    private StsSession fetchCredentials() {
        log.info("Requesting SQS credentials from the CloudCard API.")

        Credentials response = cloudCardClient.fetchStsCredentials(queueUrl)

        if (!isValid(response)) throw new IllegalStateException("CloudCard API returned incomplete SQS credentials.")

        if (!response.expiration().isAfter(clock.instant())) throw new IllegalStateException("Received SQS credentials that are already expired; check the downloader system clock and broker response.")

        AwsSessionCredentials credentials = AwsSessionCredentials.create(response.accessKeyId(), response.secretAccessKey(), response.sessionToken())

        Instant expiresAt = response.expiration()

        log.info("SQS credentials obtained. Expire at {}.", response.expiration())

        return new StsSession(credentials, expiresAt.minusSeconds(REFRESH_BUFFER_SECONDS), expiresAt)
    }

    private static boolean isValid(Credentials credentials) {
        if (credentials &&
            credentials.accessKeyId() &&
            credentials.secretAccessKey() &&
            credentials.sessionToken() &&
            credentials.expiration()) {
            return true
        }

        return false
    }

    private static final class StsSession {
        final AwsSessionCredentials credentials
        final Instant refreshAt
        final Instant expiresAt

        StsSession(AwsSessionCredentials credentials, Instant refreshAt, Instant expiresAt) {
            this.credentials = credentials
            this.refreshAt = refreshAt
            this.expiresAt = expiresAt
        }
    }
}
