package com.cloudcard.photoDownloader

class StorageResults {
    public List<PhotoFile> downloadedPhotoFiles

    /**
     *  This list is intended to show what files are not to be retried without human intervention
     */
    public List<UnsavablePhotoFile> unsavablePhotoFiles

    StorageResults(List<PhotoFile> downloadedPhotoFiles){
        this.downloadedPhotoFiles = downloadedPhotoFiles
        this.unsavablePhotoFiles = []
    }

    StorageResults(List<PhotoFile> downloadedPhotoFiles, List<UnsavablePhotoFile> unsavablePhotoFiles){
        this.downloadedPhotoFiles = downloadedPhotoFiles
        this.unsavablePhotoFiles = unsavablePhotoFiles
    }

    static StorageResults empty() { return new StorageResults([]) }

}
