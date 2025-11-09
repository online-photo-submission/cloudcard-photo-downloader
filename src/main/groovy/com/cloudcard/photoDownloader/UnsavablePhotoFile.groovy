package com.cloudcard.photoDownloader

class UnsavablePhotoFile extends PhotoFile {
    String errorMessage

    UnsavablePhotoFile(String accountId, String fileName, Long photoId, String errorMessage) {
        super(accountId, fileName, photoId as Integer)
        this.errorMessage = errorMessage
    }

    static UnsavablePhotoFile fromPhotoId(Long photoId, String errorMessage, String accountId = null) {
        new UnsavablePhotoFile(accountId, null, photoId, errorMessage)
    }

    @Override
    String toString() {
        "UnsavablePhotoFile(photoId=${photoId}, accountId=${accountId}, errorMessage=${errorMessage})"
    }
}