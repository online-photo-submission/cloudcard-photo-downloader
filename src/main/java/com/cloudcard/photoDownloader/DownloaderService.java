package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

@Service
public class DownloaderService {

    private static final Logger log = LoggerFactory.getLogger(DownloaderService.class);

    @Autowired
    CloudCardPhotoService cloudCardPhotoService;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    StorageService storageService;

    @Autowired
    ApplicationPropertiesValidator applicationPropertiesValidator;

    @PostConstruct
    public void init() {

        applicationPropertiesValidator.validate();
    }

    @Scheduled(fixedDelayString = "${downloader.delay.milliseconds}")
    public void downloadPhotos() throws Exception {

        log.info("  ==========  Downloading photos  ==========  ");
        List<Photo> photosToDownload = cloudCardPhotoService.fetchReadyForDownload();
        List<PhotoFile> downloadedPhotoFiles = storageService.save(photosToDownload);
        for (PhotoFile photoFile : downloadedPhotoFiles) {
            Photo downloadedPhoto = new Photo(photoFile.getPhotoId());
            cloudCardPhotoService.markAsDownloaded(downloadedPhoto);
        }

        log.info("Completed downloading " + downloadedPhotoFiles.size() + " photos.");
    }
}
