package com.cloudcard.photoDownloader.exception

class CredentialsBrokerException extends RuntimeException{
    int status          // 0 for network-level failures
    boolean permanent

    CredentialsBrokerException(String message, int status, boolean permanent, Throwable cause = null) {
        super(message, cause)
        this.status = status
        this.permanent = permanent
    }
}
