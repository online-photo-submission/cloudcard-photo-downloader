package com.cloudcard.photoDownloader

interface ManifestFileService {
    void createManifestFile(List<Photo> photosToDownload, List<PhotoFile> photoFiles)

}