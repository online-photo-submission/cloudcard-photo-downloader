package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DownloaderService {

    private static final Logger log = LoggerFactory.getLogger(DownloaderService.class);

    @Autowired
    CloudCardPhotoService cloudCardPhotoService;

    @Autowired
    StorageService storageService;

    @Autowired
    AppilcationPropertiesValidator appilcationPropertiesValidator;

    @Scheduled(fixedDelayString = "${downloader.delay.milliseconds}")
    public void downloadPhotos() throws Exception {

        appilcationPropertiesValidator.validate();

        log.info("Downloading photos...");
        List<PhotoFile> downloadedPhotos = storageService.save(cloudCardPhotoService.fetchReadyForDownload());
        for (PhotoFile photo : downloadedPhotos) {
            cloudCardPhotoService.markAsDownloaded(new Photo(photo.getPhotoId()));
        }

        log.info("Completed downloading " + downloadedPhotos.size() + " photos.");
    }
}
