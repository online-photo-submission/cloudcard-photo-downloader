package com.cloudcard.photoDownloader.exception

/**
 * A non-success response from the CloudCard API.
 *
 * `permanent` is what the Retry predicate keys on: auth, authorisation and bad-request failures
 * will not succeed on a second attempt, so they propagate immediately rather than burning attempts.
 */
class CloudCardApiException extends RuntimeException {

    /** HTTP status from the CloudCard API, or 0 for network-level failures. */
    final int status

    /** True when retrying cannot help. */
    final boolean permanent

    CloudCardApiException(String message, int status, boolean permanent, Throwable cause = null) {
        super(message, cause)
        this.status = status
        this.permanent = permanent
    }
}