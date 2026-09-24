package com.cloudcard.photoDownloader

import com.cloudcard.photoDownloader.exception.CloudCardApiException
import io.github.resilience4j.core.IntervalFunction
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sts.model.Credentials

/**
 * Policy layer for the CloudCard API: retry, token handling, orchestration.
 * All HTTP lives in RemotePhotoUtil.
 */
@Component
class CloudCardClient {

    private static final Logger log = LoggerFactory.getLogger(CloudCardClient.class)

    public static final String READY_FOR_DOWNLOAD = "READY_FOR_DOWNLOAD"
    public static final String APPROVED = "APPROVED"
    public static final String DOWNLOADED = "DOWNLOADED"
    public static final String ON_HOLD = "ON_HOLD"
    public static final String FAILED = "FAILED"

    @Value('${cloudcard.api.url}')
    private String apiUrl

    @Value('${cloudcard.api.retry.maxAttempts:3}')
    private int maxAttempts

    @Value('${cloudcard.api.retry.initialIntervalMillis:1000}')
    private long initialIntervalMillis

    @Value('${cloudcard.api.retry.maxIntervalMillis:30000}')
    private long maxIntervalMillis

    @Autowired
    TokenService tokenService

    @Autowired
    PreProcessor preProcessor

    Retry retry

    @PostConstruct
    void init() {
        log.info("                    API URL : " + apiUrl)
        log.info("              Pre-Processor : " + preProcessor.getClass().getSimpleName())
        log.info("              API Retry     : ${maxAttempts} attempts, ${initialIntervalMillis}ms to ${maxIntervalMillis}ms")

        RetryConfig retryConfig = RetryConfig.custom()
            .maxAttempts(maxAttempts)
            .intervalFunction(IntervalFunction.ofExponentialRandomBackoff(
                initialIntervalMillis, 2.0d, 0.25d, maxIntervalMillis))
            .retryOnException(this.&isRetryable)
            .failAfterMaxAttempts(true)
            .build()

        retry = Retry.of("CloudCardClient", retryConfig)

        retry.eventPublisher.onRetry { event ->
            log.warn("Retrying CloudCard API call, attempt ${event.numberOfRetryAttempts}.")
        }
    }

    /**
     * Retry transient API failures and anything network-level. A permanent failure -- auth,
     * authorisation, a bad request -- will not succeed on a second attempt, so it propagates
     * immediately.
     */
    private boolean isRetryable(Throwable throwable) {
        if (throwable instanceof CloudCardApiException) {
            return !((CloudCardApiException) throwable).permanent
        }

        return throwable instanceof IOException
    }

    boolean isConfigured() {
        return apiUrl && tokenService.isConfigured()
    }

    /* *** PHOTOS *** */

    List<Photo> fetchWithBytes(String[] fetchStatuses) throws Exception {
        List<Photo> photos = fetch(fetchStatuses)

        for (Photo photo : photos) {
            Photo processedPhoto = preProcessor.process(photo)
            fetchBytes(processedPhoto)
        }

        return photos
    }

    void fetchBytes(Photo photo) throws Exception {
        photo.bytes = withRetry { RemotePhotoUtil.fetchBytes(photo.externalURL) }
    }

    void fetchBytes(AdditionalPhoto additionalPhoto) throws Exception {
        additionalPhoto.bytes = withRetry { RemotePhotoUtil.fetchBytes(additionalPhoto.externalURL) }
    }

    List<Photo> fetch(String[] statuses) throws Exception {
        List<Photo> photos = []

        for (String status : statuses) {
            photos.addAll(fetch(status))
        }

        return photos
    }

    List<Photo> fetch(String status) throws Exception {
        return withRetry {
            RemotePhotoUtil.fetchPhotos(apiUrl, tokenService.authTokenValue, status)
        }
    }

    Photo updateStatus(Photo photo, String status, String message = null) throws Exception {
        return withRetry {
            RemotePhotoUtil.updateStatus(apiUrl, tokenService.authTokenValue, photo, status, message)
        }
    }

    /* *** CREDENTIAL BROKERING *** */

    Credentials fetchStsCredentials(String queueUrl) throws Exception {
        return withRetry {
            RemotePhotoUtil.fetchStsCredentials(apiUrl, tokenService.authTokenValue, queueUrl)
        }
    }

    /* *** LIFECYCLE *** */

    void close() {

    }

    private <T> T withRetry(Closure<T> call) {
        return Retry.decorateSupplier(retry, call).get()
    }
}
