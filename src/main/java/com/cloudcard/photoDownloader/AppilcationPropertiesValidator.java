package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.management.timer.Timer;

@Component
public class AppilcationPropertiesValidator {

    private static final Logger log = LoggerFactory.getLogger(AppilcationPropertiesValidator.class);

    @Value("${downloader.delay.milliseconds}")
    private Integer downloaderDelay;

    @Value("${cloudcard.api.url}")
    private String apiUrl;

    @Value("${cloudcard.api.accessToken}")
    private String accessToken;

    @Value("${downloader.photoDirectory}")
    String photoDirectory;

    @Value("${downloader.enableUdf}")
    private Boolean enableUdf;

    @Value("${downloader.udfDirectory}")
    String udfDirectory;

    @Value("${downloader.udfFilePrefix}")
    private String udfFilePrefix;

    @Value("${downloader.udfFileExtension}")
    private String udfFileExtension;

    @Value("${downloader.descriptionDateFormat}")
    private String descriptionDateFormat;

    @Value("${downloader.batchIdDateFormat}")
    private String batchIdDateFormat;

    @Value("${downloader.createdDateFormat}")
    private String createdDateFormat;

    public void validate() {

        throwIfTrue(downloaderDelay < Timer.ONE_MINUTE, "The minimum downloader delay is 60000 milliseconds (one minute).");
        throwIfBlank(apiUrl, "The CloudCard API URL must be specified.");
        throwIfBlank(accessToken, "The CloudCard API access token must be specified.");
        throwIfBlank(photoDirectory, "The Photo Directory must be specified.");

        if (enableUdf) {
            throwIfBlank(udfDirectory, "The UDF Directory must be specified.");
            throwIfBlank(udfFilePrefix, "The UDF File Prefix must be specified.");
            throwIfBlank(udfFileExtension, "The UDF File Extension must be specified.");
            throwIfBlank(descriptionDateFormat, "The Description Date Format must be specified.");
            throwIfBlank(batchIdDateFormat, "The Batch ID Date Format must be specified.");
            throwIfBlank(createdDateFormat, "The Created Date Format must be specified.");
        }

    }

    private void throwIfBlank(String string, String message) {

        throwIfTrue(string == null || string.isEmpty(), message);
    }

    private void throwIfTrue(boolean condition, String message) {

        if (condition) {
            log.error(message);
            throw new ApplicationPropertiesException(message);
        }
    }
}
