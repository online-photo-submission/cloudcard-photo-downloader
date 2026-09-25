package com.cloudcard.photoDownloader;

import jakarta.annotation.PostConstruct;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.*;

@Service
public class DownloaderService {

    private static final Logger log = LoggerFactory.getLogger(DownloaderService.class);

    private static final long BASE_BACKOFF_SECONDS = 2;
    private static final long MAX_BACKOFF_SECONDS = 600;
    private int consecutiveFailures = 0;

    private static final Duration REMOTE_CONFIG_CHECK_INTERVAL = Duration.ofMinutes(5);
    private Instant nextRemoteConfigCheckAt = Instant.MIN;

    @Autowired
    PhotoService photoService;

    @Autowired
    StorageService storageService;

    @Autowired
    ManifestFileService manifestFileService;

    @Autowired
    SummaryService summaryService;

    @Autowired
    ShellCommandService shellCommandService;

    @Autowired(required = false)
    RemoteConfigService remoteConfigService;

    @Value("${downloader.useRemoteConfigs:false}")
    private boolean useRemoteConfigs;

    @Value("${downloader.delay.milliseconds}")
    private Integer downloaderDelay;

    @Value("${downloader.scheduling.type}")
    private String schedulingType;

    @Value("${downloader.cron.schedule}")
    private String cronSchedule;

    @Value("${downloader.repeat:true}")
    private boolean repeat;

    @Value("${downloader.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${downloader.proxy.port:0}")
    private int proxyPort;

    @PostConstruct
    public void init() {

        throwIfTrue(downloaderDelay < photoService.minDownloaderDelay(),"The minimum downloader delay is " + photoService.minDownloaderDelay() + " milliseconds.");
        throwIfTrue(storageService == null, "The Storage Service must be specified.");
        throwIfTrue(summaryService == null, "The Summary Service must be specified.");

        logVersion();
        logScheduleSettings(schedulingType, repeat, downloaderDelay, cronSchedule);
        log.info("      Storage Service : " + storageService.getClass().getSimpleName());
        log.info("      Summary Service : " + summaryService.getClass().getSimpleName());
        log.info("Manifest File Service : " + manifestFileService.getClass().getSimpleName());

        if (proxyHost != null && proxyPort > 0) {
            log.info("          Using Proxy : " + proxyHost + ":" + proxyPort);
            Unirest.config().proxy(proxyHost, proxyPort);
        }
    }

    @Scheduled(fixedDelayString = "${downloader.delay.milliseconds}", initialDelayString = "5000")
    public void downloadPhotosFixedDelay() throws Exception {
        if (schedulingType.equals("fixedDelay")) {
            downloadPhotos();
        }
    }

    @Scheduled(cron = "${downloader.cron.schedule}" )
    public void downloadPhotosCron() throws Exception {
        if (schedulingType.equals("cron")) {
            downloadPhotos();
        }
    }

    public void downloadPhotos() throws Exception {

        int exitStatus = 0;
        long backoffSeconds = 0;

//TODO: Make sure this isn't too complicated
        if (useRemoteConfigs &&
            shouldCheckRemoteConfig() &&
            remoteConfigService.isUpdated()) {
            log.info("New configuration version detected. Restarting application to apply new settings.");
            Application.restart();

            return;
        }

        try {

            log.info("  ==========  Downloading photos  ==========  ");
            shellCommandService.preExecute();
            List<Photo> photosToDownload = photoService.fetchReadyForDownload();
            shellCommandService.preDownload(photosToDownload);
            StorageResults results = storageService.save(photosToDownload);
            for (PhotoFile photoFile : results.downloadedPhotoFiles) {
                Photo downloadedPhoto = new Photo(photoFile.getPhotoId());
                photoService.markAsDownloaded(downloadedPhoto);
            }
            for (FailedPhotoFile failedPhotoFile : results.failedPhotoFiles) {
                Photo failedPhoto = new Photo(failedPhotoFile.getPhotoId());
                photoService.markAsFailed(failedPhoto, failedPhotoFile.getErrorMessage());
            }
            manifestFileService.createManifestFile(photosToDownload, results.downloadedPhotoFiles);
            summaryService.createSummary(photosToDownload, results.downloadedPhotoFiles);
            shellCommandService.postDownload(results.downloadedPhotoFiles);
            shellCommandService.postExecute();
            log.info("Completed downloading " + results.downloadedPhotoFiles.size() + " photos.");
            consecutiveFailures = 0;

        } catch (Exception e) {

            log.error(e.getMessage());
            e.printStackTrace();
            exitStatus = 1;

            consecutiveFailures++;
            backoffSeconds = backoffSecondsFor(consecutiveFailures);

        } finally {
            try {
                photoService.close();
            } catch (Exception e) {
                log.error("Failed to close the photoService: ");
                log.error(e.getMessage());
                log.debug(Arrays.toString(e.getStackTrace()));
            }

            if (!repeat) {
                log.info("downloader.repeat is set to false. Exiting application now.");
                System.exit(exitStatus);
            }

//            TODO: backoff *could* cause minor issues when debugging and slow feedback loops... but we also don't want to have EVERY downloader
//                  blasting the system at the same time if the API went down and these all started failing.
//            Only implement backoff if delay is set for less than 10 minutes.
            if (backoffSeconds > 0 && (downloaderDelay < 600000 || schedulingType.equals("cron"))) {
                log.warn("Backing off " + backoffSeconds + "s after " + consecutiveFailures + " consecutive failures.");
                Thread.sleep(backoffSeconds * 1000L);
            }
        }
    }

    /** 2s, 4s, 8s ... capped at 600s, with +/-25% jitter so a fleet-wide failure doesn't retry in lockstep. */
    private long backoffSecondsFor(int failures) {
        int exponent = Math.min(failures, 10);
        long seconds = Math.min(BASE_BACKOFF_SECONDS << (exponent - 1), MAX_BACKOFF_SECONDS);
        double jitter = 0.75 + (ThreadLocalRandom.current().nextDouble() * 0.5);
        return Math.max(1L, (long) (seconds * jitter));
    }

    private synchronized boolean shouldCheckRemoteConfig() {
        Instant now = Instant.now();

        if (now.isBefore(nextRemoteConfigCheckAt)) {
            return false;
        }

        // Throttle failed attempts too.
        nextRemoteConfigCheckAt = now.plus(REMOTE_CONFIG_CHECK_INTERVAL);

        return true;
    }
}
