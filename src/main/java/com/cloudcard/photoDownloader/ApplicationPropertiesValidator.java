package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.management.timer.Timer;

@Component
public class ApplicationPropertiesValidator {

    private static final Logger log = LoggerFactory.getLogger(ApplicationPropertiesValidator.class);

    String version = "20.02.21";

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

    @Value("${downloader.fileNameResolver}")
    String fileNameResolver;

    @Value("${downloader.udfDirectory}")
    String udfDirectory;

    @Value("${downloader.csvDirectory}")
    String csvDirectory;

    public void validate() {

        throwIfTrue(downloaderDelay < Timer.ONE_MINUTE, "The minimum downloader delay is 60000 milliseconds (one minute).");
        throwIfBlank(apiUrl, "The CloudCard API URL must be specified.");
        throwIfBlank(accessToken, "The CloudCard API access token must be specified.");
        throwIfBlank(storageService, "The Storage Service must be specified.");
        throwIfBlank(fileNameResolver, "The File Name Resolver must be specified.");
        throwIfTrue(photoDirectories == null || photoDirectories.length == 0, "The Photo Directory(ies) must be specified.");


        logConfigValues();
    }

    private void logConfigValues() {

        log.info("========== Configuration Information ==========");
        log.info("              Version : " + version);
        log.info("                ---------------                ");
        log.info("         Access Token : " + "..." + accessToken.substring(2, 6) + "...");
        log.info("     Downloader Delay : " + downloaderDelay / 60000 + " min(s)");
        log.info("              API URL : " + apiUrl);
        log.info("                ---------------                ");
        log.info("      Storage Service : " + storageService);
        log.info("   File Name Resolver : " + fileNameResolver);
        log.info(" Photo Directory(ies) : " + String.join(" , ", photoDirectories));
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
