package com.cloudcard.photoDownloader

class FailedPhotoFile extends PhotoFile {
    String errorMessage

    FailedPhotoFile(String accountId, String fileName, Long photoId, String errorMessage) {
        super(accountId, fileName, photoId as Integer)
        this.errorMessage = errorMessage
    }

    static FailedPhotoFile fromPhotoId(Long photoId, String errorMessage, String accountId = null) {
        new FailedPhotoFile(accountId, null, photoId, errorMessage)
    }

    @Override
    String toString() {
        "FailedPhotoFile(photoId=${photoId}, accountId=${accountId}, errorMessage=${errorMessage})"
    }
}