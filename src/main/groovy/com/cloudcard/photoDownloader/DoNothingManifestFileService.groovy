package com.cloudcard.photoDownloader


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = "ManifestFileService", havingValue = "DoNothingManifestFileService", matchIfMissing = true)
class DoNothingManifestFileService implements ManifestFileService {

    @Override
    void createManifestFile(List<Photo> photosToDownload, List<PhotoFile> photoFiles) {
        // Do nothing
    }
}