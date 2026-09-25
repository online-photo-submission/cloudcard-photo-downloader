package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadService.class);

    @Autowired
    PhotoService photoService

    @Autowired
    StorageService storageService

    @Autowired
    ManifestFileService manifestFileService

    @Autowired
    SummaryService summaryService

    @Autowired
    ShellCommandService shellCommandService

    @PostConstruct
    void logDependencies() {
        log.info("              Photo Service : ${photoService.class.simpleName}")
        log.info("           Storage Service  : ${storageService.class.simpleName}")
        log.info("      Manifest File Service : ${manifestFileService.class.simpleName}")
        log.info("           Summary Service  : ${summaryService.class.simpleName}")
        log.info("      Shell Command Service : ${shellCommandService.class.simpleName}")
    }

    void downloadPhotos() throws Exception {
        try {
            log.info("  ==========  Downloading photos  ==========  ");

            shellCommandService.preExecute()
            List<Photo> photosToDownload = photoService.fetchReadyForDownload()
            shellCommandService.preDownload(photosToDownload)
            StorageResults results = storageService.save(photosToDownload)

            for (PhotoFile photoFile : results.downloadedPhotoFiles) {
                Photo photo = new Photo(photoFile.getPhotoId())
                photoService.markAsDownloaded(photo)
            }

            for (FailedPhotoFile failedPhotoFile : results.failedPhotoFiles) {
                Photo photo = new Photo(failedPhotoFile.getPhotoId())
                photoService.markAsFailed(photo, failedPhotoFile.getErrorMessage())
            }

            manifestFileService.createManifestFile(photosToDownload, results.downloadedPhotoFiles)
            summaryService.createSummary(photosToDownload, results.downloadedPhotoFiles)
            shellCommandService.postDownload(results.downloadedPhotoFiles)
            shellCommandService.postExecute();

            log.info("Completed downloading ${results.downloadedPhotoFiles.size()} photos.")
        } finally {
            try {
                photoService.close()
            } catch (Exception e) {
                log.error("Failed to close the photo service.", e)
            }
        }
    }
}
