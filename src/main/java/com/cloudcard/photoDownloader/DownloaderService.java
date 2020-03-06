package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.management.timer.Timer;
import java.util.List;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.logVersion;
import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue;

@Service
public class DownloaderService {

    private static final Logger log = LoggerFactory.getLogger(DownloaderService.class);

    @Autowired
    CloudCardPhotoService cloudCardPhotoService;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    StorageService storageService;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    SummaryService summaryService;

    @Autowired
    ShellCommandService shellCommandService;

    @Value("${downloader.delay.milliseconds}")
    private Integer downloaderDelay;

    @Value("${downloader.repeat:true}")
    private boolean repeat;

    @PostConstruct
    public void init() {

        throwIfTrue(downloaderDelay < Timer.ONE_MINUTE, "The minimum downloader delay is 60000 milliseconds (one minute).");
        throwIfTrue(storageService == null, "The Storage Service must be specified.");
        throwIfTrue(summaryService == null, "The Summary Service must be specified.");

        logVersion();
        log.info("     Downloader Delay : " + downloaderDelay / 60000 + " min(s)");
        log.info("      Storage Service : " + storageService);
        log.info("      Summary Service : " + summaryService);
    }

    @Scheduled(fixedDelayString = "${downloader.delay.milliseconds}")
    public void downloadPhotos() throws Exception {

        log.info("  ==========  Downloading photos  ==========  ");
        shellCommandService.preExecute();
        List<Photo> photosToDownload = cloudCardPhotoService.fetchReadyForDownload();
        shellCommandService.preDownload(photosToDownload);
        List<PhotoFile> downloadedPhotoFiles = storageService.save(photosToDownload);
        for (PhotoFile photoFile : downloadedPhotoFiles) {
            Photo downloadedPhoto = new Photo(photoFile.getPhotoId());
            cloudCardPhotoService.markAsDownloaded(downloadedPhoto);
        }

        summaryService.createSummary(photosToDownload, downloadedPhotoFiles);
        shellCommandService.postDownload(downloadedPhotoFiles);
        shellCommandService.postExecute();
        log.info("Completed downloading " + downloadedPhotoFiles.size() + " photos.");

        if (!repeat) {
            log.info("downloader.repeat is set to false. Exiting application now.");
            System.exit(0);
        }
    }
}
