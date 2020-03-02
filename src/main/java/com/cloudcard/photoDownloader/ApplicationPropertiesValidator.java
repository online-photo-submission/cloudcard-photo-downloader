package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationPropertiesValidator {

    private static final Logger log = LoggerFactory.getLogger(ApplicationPropertiesValidator.class);

    public static String version = "20.03.02";

    public static void logVersion() {

        log.info("  Application Version : " + version);
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
