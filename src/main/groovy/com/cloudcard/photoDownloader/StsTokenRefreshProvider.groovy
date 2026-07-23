package com.cloudcard.photoDownloader

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.auth.credentials.AwsCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials
import software.amazon.awssdk.services.sts.model.Credentials

import java.time.Instant

//TODO: Test this
class StsTokenRefreshingProvider implements AwsCredentialsProvider {
    private static final Logger log = LoggerFactory.getLogger(StsTokenRefreshingProvider)
    private static final int REFRESH_BUFFER_SECONDS = 300 // 5 minutes before expiration

    private final CloudCardClient cloudCardClient
    private final String queueUrl

    // Thread-safe caching state
    private volatile AwsSessionCredentials cachedCredentials
    private volatile Instant expirationTime

    StsTokenRefreshingProvider(CloudCardClient cloudCardClient, String queueUrl) {
        this.cloudCardClient = cloudCardClient
        this.queueUrl = queueUrl
    }

    @Override
    AwsCredentials resolveCredentials() {
        if (shouldRefresh()) {
            synchronized (this) {
                if (shouldRefresh()) {
                    refreshCredentials()
                }
            }
        }
        return cachedCredentials
    }

    private boolean shouldRefresh() {
        if (!cachedCredentials || !expirationTime) return true
        // Refresh proactively if we are within the 5-minute safety buffer window
        return Instant.now().plusSeconds(REFRESH_BUFFER_SECONDS).isAfter(expirationTime)
    }

    private void refreshCredentials() {
        log.info("Requesting fresh short-lived STS tokens from CloudCard API...")
        try {
            // 1. Explicitly type it to the AWS SDK Credentials object returned by Option 2
            Credentials credentials = cloudCardClient.fetchStsCredentials(queueUrl)

            // 2. Use the AWS SDK native method getters instead of string map keys
            this.cachedCredentials = AwsSessionCredentials.builder()
                .accessKeyId(credentials.accessKeyId())
                .secretAccessKey(credentials.secretAccessKey())
                .sessionToken(credentials.sessionToken())
                .build()

            // 3. The AWS Credentials object natively holds the expiration as an Instant object!
            // No more manual string parsing or string matching required.
            this.expirationTime = credentials.expiration()

            log.info("STS credentials refreshed successfully. Next expiry: ${expirationTime}")
        } catch (Exception e) {
            log.error("Failed to refresh temporary AWS credentials!", e)
            if (cachedCredentials) {
                log.warn("Attempting to fall back to expired in-memory cache to maintain continuity.")
                return
            }
            throw new IllegalStateException("Initial downloader authentication failed; no credentials available.", e)
        }
    }
}