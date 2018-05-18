package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.management.timer.Timer;
import java.util.List;

@Service
public class DownloaderService {

    private static final Logger log = LoggerFactory.getLogger(DownloaderService.class);

    @Autowired
    CloudCardPhotoService cloudCardPhotoService;

    @Autowired
    StorageService storageService;

    @Value("${downloader.delay.milliseconds}")
    private Integer downloaderDelay;

    @Scheduled(fixedDelayString = "${downloader.delay.milliseconds}")
    public void downloadPhotos() throws Exception {

        if (downloaderDelay < Timer.ONE_MINUTE) {
            String msg = "The minimum downloader delay is 60000 milliseconds (one minute).  Please update the application.properties file.";
            log.error(msg);
            throw new RuntimeException(msg);
        }

        log.info("Downloading photos...");
        List<PhotoFile> downloadedPhotos = storageService.save(cloudCardPhotoService.fetchReadyForDownload());
        for (PhotoFile photo : downloadedPhotos) {
            cloudCardPhotoService.markAsDownloaded(new Photo(photo.getPhotoId()));
        }

        log.info("Completed downloading " + downloadedPhotos.size() + " photos.");
    }
}
