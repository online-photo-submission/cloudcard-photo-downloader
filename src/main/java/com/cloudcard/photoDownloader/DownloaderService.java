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
    StorageService storageService;

    @Autowired
    ApplicationPropertiesValidator applicationPropertiesValidator;

    @PostConstruct
    public void init() {

        applicationPropertiesValidator.validate();

        log.info("========== Application Information ==========");
        log.info("         Access Token : " + applicationPropertiesValidator.accessToken.substring(0, 4) + "****");
        log.info("Wildcard Photo Folder : " + applicationPropertiesValidator.photoDirectoryWildcard);
        log.info(" Outlook Photo Folder : " + applicationPropertiesValidator.photoDirectoryOutlook);
        log.info("          DB filepath : " + applicationPropertiesValidator.photoFieldFilePath);
        log.info("======== End Application Information ========");
    }

    @Scheduled(fixedDelayString = "${downloader.delay.milliseconds}")
    public void downloadPhotos() throws Exception {

        log.info("  ==========  Downloading photos  ==========  ");
        List<PhotoFile> downloadedPhotos = storageService.save(cloudCardPhotoService.fetchReadyForDownload());
        for (PhotoFile photo : downloadedPhotos) {
            cloudCardPhotoService.markAsDownloaded(new Photo(photo.getPhotoId()));
        }

        log.info("Completed downloading " + downloadedPhotos.size() + " photos.");
    }
}
