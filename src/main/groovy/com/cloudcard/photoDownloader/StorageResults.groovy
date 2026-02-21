package com.cloudcard.photoDownloader

class StorageResults {
    public List<PhotoFile> downloadedPhotoFiles

    /**
     *  This list is intended to show what files are not to be retried without human intervention
     *  e.g. if the photo is missing a person, then we put the photo on hold with an appropriate reason.
     */
    public List<FailedPhotoFile> failedPhotoFiles

    StorageResults(List<PhotoFile> downloadedPhotoFiles) {
        this.downloadedPhotoFiles = downloadedPhotoFiles
        this.failedPhotoFiles = []
    }

    StorageResults(List<PhotoFile> downloadedPhotoFiles, List<FailedPhotoFile> failedPhotoFiles) {
        this.downloadedPhotoFiles = downloadedPhotoFiles
        this.failedPhotoFiles = failedPhotoFiles
    }

    static StorageResults empty() { return new StorageResults([]) }

}
