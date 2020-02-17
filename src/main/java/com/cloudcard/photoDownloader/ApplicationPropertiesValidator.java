package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.management.timer.Timer;

@Component
public class ApplicationPropertiesValidator {

    private static final Logger log = LoggerFactory.getLogger(ApplicationPropertiesValidator.class);

    @Value("${downloader.delay.milliseconds}")
    private Integer downloaderDelay;

    @Value("${cloudcard.api.url}")
    private String apiUrl;

    @Value("${cloudcard.api.accessToken}")
    String accessToken;

    @Value("${downloader.photoDirectories}")
    String[] photoDirectories;

    // TODO: Get rid of these one-off directories
    @Value("${downloader.photoDirectoryWildcard}")
    String photoDirectoryWildcard;
    @Value("${downloader.photoDirectoryOutlook}")
    String photoDirectoryOutlook;
    @Value("${downloader.photoDirectoryError}")
    String photoDirectoryError;
    @Value("${downloader.sql.photoField.filePath:}")
    String photoFieldFilePath;

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
        throwIfTrue(photoDirectories == null || photoDirectories.length == 0, "The Photo Directory(ies) must be specified.");
        throwIfTrue(photoDirectoryWildcard == null || photoDirectoryOutlook == null || photoDirectoryError == null, "The Photo Directories must be specified.");

        if (enableUdf) {
            throwIfBlank(udfDirectory, "The UDF Directory must be specified.");
            throwIfBlank(udfFilePrefix, "The UDF File Prefix must be specified.");
            throwIfBlank(udfFileExtension, "The UDF File Extension must be specified.");
            throwIfBlank(descriptionDateFormat, "The Description Date Format must be specified.");
            throwIfBlank(udfBatchIdDateFormat, "The Batch ID Date Format must be specified.");
            throwIfBlank(createdDateFormat, "The Created Date Format must be specified.");

            throwIfBlank(csvDirectory, "The CSV Directory must be specified.");
            throwIfBlank(csvFilePrefix, "The CSV File Prefix must be specified.");
            throwIfBlank(csvFileExtension, "The CSV File Extension must be specified.");
            throwIfBlank(csvBatchIdDateFormat, "The Batch ID Date Format must be specified.");
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
