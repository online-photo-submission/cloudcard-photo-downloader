package com.cloudcard.photoDownloader;

import com.mashape.unirest.http.Unirest;
import org.apache.http.HttpHost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.logVersion;
import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue;

@Service
public class DownloaderService {

    private static final Logger log = LoggerFactory.getLogger(DownloaderService.class);

    @Autowired
    PhotoService photoService;

    @Autowired
    TokenService tokenService;

    @Autowired
    StorageService storageService;

    @Autowired
    SummaryService summaryService;

    @Autowired
    ShellCommandService shellCommandService;

    @Value("${downloader.delay.milliseconds}")
    private Integer downloaderDelay;

    @Value("${downloader.repeat:true}")
    private boolean repeat;

    @Value("${downloader.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${downloader.proxy.port:0}")
    private int proxyPort;

    @PostConstruct
    public void init() {

        throwIfTrue(downloaderDelay < photoService.minDownloaderDelay(), "The minimum downloader delay is " + photoService.minDownloaderDelay() + " milliseconds.");
        throwIfTrue(storageService == null, "The Storage Service must be specified.");
        throwIfTrue(summaryService == null, "The Summary Service must be specified.");

        logVersion();
        log.info("          Repeat Mode : " + (repeat ? "Run Continually" : "Run Once & Stop"));
        log.info("     Downloader Delay : " + downloaderDelay / 60000 + " min(s)");
        log.info("      Storage Service : " + storageService.getClass().getSimpleName());
        log.info("      Summary Service : " + summaryService.getClass().getSimpleName());

        if (proxyHost != null && proxyPort > 0) {
            log.info("          Using Proxy : " + proxyHost + ":" + proxyPort);
            Unirest.setProxy(new HttpHost(proxyHost, proxyPort));
        }
    }

    @Scheduled(fixedDelayString = "${downloader.delay.milliseconds}")
    public void downloadPhotos() throws Exception {

        int exitStatus = 0;

        try {

            log.info("  ==========  Downloading photos  ==========  ");
            tokenService.login();
            shellCommandService.preExecute();
            List<Photo> photosToDownload = photoService.fetchReadyForDownload();
            shellCommandService.preDownload(photosToDownload);
            List<PhotoFile> downloadedPhotoFiles = storageService.save(photosToDownload);
            for (PhotoFile photoFile : downloadedPhotoFiles) {
                Photo downloadedPhoto = new Photo(photoFile.getPhotoId());
                photoService.markAsDownloaded(downloadedPhoto);
            }

            summaryService.createSummary(photosToDownload, downloadedPhotoFiles);
            shellCommandService.postDownload(downloadedPhotoFiles);
            shellCommandService.postExecute();
            log.info("Completed downloading " + downloadedPhotoFiles.size() + " photos.");
            tokenService.logout();

        } catch (Exception e) {

            log.error(e.getMessage());
            e.printStackTrace();
            exitStatus = 1;

        } finally {

            if (!repeat) {
                log.info("downloader.repeat is set to false. Exiting application now.");
                System.exit(exitStatus);
            }
        }
    }
}
