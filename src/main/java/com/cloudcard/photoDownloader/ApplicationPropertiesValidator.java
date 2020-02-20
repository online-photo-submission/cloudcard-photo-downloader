package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.management.timer.Timer;

@Component
public class ApplicationPropertiesValidator {

    private static final Logger log = LoggerFactory.getLogger(ApplicationPropertiesValidator.class);

    String version = "20.02.20";

    @Value("${downloader.delay.milliseconds}")
    private Integer downloaderDelay;

    @Value("${cloudcard.api.url}")
    private String apiUrl;

    @Value("${cloudcard.api.accessToken}")
    String accessToken;

    @Value("${downloader.photoDirectories}")
    String[] photoDirectories;

    @Value("${downloader.storageService}")
    String storageService;

    // TODO: Remove this; build it into the custom query
    @Value("${downloader.sql.photoField.filePath:}")
    String photoFieldFilePath;

    @Value("${downloader.udfDirectory}")
    String udfDirectory;

    @Value("${downloader.udfFilePrefix}")
    private String udfFilePrefix;

    @Value("${downloader.udfFileExtension}")
    private String udfFileExtension;

    @Value("${downloader.descriptionDateFormat}")
    private String descriptionDateFormat;

    @Value("${downloader.udfBatchIdDateFormat}")
    private String udfBatchIdDateFormat;

    @Value("${downloader.createdDateFormat}")
    private String createdDateFormat;

    @Value("${downloader.enableCsv}")
    private boolean enableCsv;

    @Value("${downloader.csvDirectory}")
    String csvDirectory;

    @Value("${downloader.csvFilePrefix}")
    private String csvFilePrefix;

    @Value("${downloader.csvFileExtension}")
    private String csvFileExtension;

    @Value("${downloader.csvBatchIdDateFormat}")
    private String csvBatchIdDateFormat;

    public void validate() {

        throwIfTrue(downloaderDelay < Timer.ONE_MINUTE, "The minimum downloader delay is 60000 milliseconds (one minute).");
        throwIfBlank(apiUrl, "The CloudCard API URL must be specified.");
        throwIfBlank(accessToken, "The CloudCard API access token must be specified.");
        throwIfBlank(storageService, "The Storage Service must be specified.");
        throwIfTrue(photoDirectories == null || photoDirectories.length == 0, "The Photo Directory(ies) must be specified.");


        logConfigValues();
    }

    private void logConfigValues() {

        log.info("========== Configuration Information ==========");
        log.info("              Version : " + version);
        log.info("                ---------------                ");
        log.info("         Access Token : " + "..." + accessToken.substring(2, 6) + "...");
        log.info("     Downloader Delay : " + downloaderDelay / 1000 + " secs");
        log.info("              API URL : " + apiUrl);
        log.info("                ---------------                ");
        log.info("      Storage Service : " + storageService);
        log.info(" Photo Directory(ies) : " + String.join(" , ", photoDirectories));
        log.info("          DB filepath : " + photoFieldFilePath);
        log.info("======== End Configuration Information ========");
    }

    public static void throwIfBlank(String string, String message) {

        throwIfTrue(string == null || string.isEmpty(), message);
    }

    public static void throwIfTrue(boolean condition, String message) {

        if (condition) {
            log.error(message);
            throw new ApplicationPropertiesException(message);
        }
    }
}
