package com.cloudcard.photoDownloader;

import io.github.resilience4j.core.IntervalFunction;
import jakarta.annotation.PostConstruct;
import kong.unirest.core.Config;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.*;

@Service
public class DownloadRunnerService {

    private static final Logger log = LoggerFactory.getLogger(DownloadRunnerService.class);

    private static final IntervalFunction BACKOFF =
            IntervalFunction.ofExponentialRandomBackoff(
                    Duration.ofSeconds(2),   // initial
                    2.0d,                    // multiplier
                    0.25d,                   // randomization factor
                    Duration.ofSeconds(600)  // max
            );

    private int consecutiveFailures = 0;

    @Autowired
    PhotoService photoService;

    @Autowired
    DownloadService downloadService;

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
        logVersion();
        logScheduleSettings(schedulingType, repeat, downloaderDelay, cronSchedule);

        configureUnirest();
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

        if (useRemoteConfigs && remoteConfigService.shouldRefresh()) {
            log.info("New configuration version detected. Restarting application to apply new settings.");
            Application.restart();

            return;
        }

        try {
            downloadService.downloadPhotos();
            consecutiveFailures = 0;

        } catch (Exception e) {
            log.error(e.getMessage());
            e.printStackTrace();
            exitStatus = 1;

            consecutiveFailures++;

        } finally {
            if (!repeat) exit(exitStatus);

            if (consecutiveFailures > 0) backoff(consecutiveFailures);
        }
    }

    private void configureUnirest() {
        Config config = Unirest.config()
                .reset()
                .connectTimeout(10_000)
                .requestTimeout(120_000);

        if (proxyHost != null && proxyPort > 0) {
            log.info("          Using Proxy : " + proxyHost + ":" + proxyPort);
            config.proxy(proxyHost, proxyPort);
        }
    }

    private static void exit(int exitStatus) {
        log.info("downloader.repeat is set to false. Exiting application now.");
        System.exit(exitStatus);

    }

    private static void backoff(int consecutiveFailures) throws Exception {
        long backoffMillis = BACKOFF.apply(consecutiveFailures);

        log.warn("Backing off " + backoffMillis + "ms after " + consecutiveFailures + " consecutive failures.");
        Thread.sleep(backoffMillis);
    }
}
