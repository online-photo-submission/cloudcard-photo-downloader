package com.cloudcard.photoDownloader

class FailedPhotoFile extends PhotoFile {
    String errorMessage

    FailedPhotoFile(Long photoId, String errorMessage) {
        super(null, null, photoId as Integer)
        this.errorMessage = errorMessage
    }

    static FailedPhotoFile fromPhotoId(Long photoId, String errorMessage) {
        new FailedPhotoFile(photoId, errorMessage)
    }

    @Override
    String toString() {
        "FailedPhotoFile(photoId=${photoId}, errorMessage=${errorMessage})"
    }
}
