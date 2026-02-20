package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplicationPropertiesValidator {

    private static final Logger log = LoggerFactory.getLogger(ApplicationPropertiesValidator.class);

    public static String version = "26.02.20.1523";

    public static void logVersion() {

        log.info("  Application Version : " + version);
    }

    public static void logScheduleSettings(String scheduleType, boolean repeat, int downloaderDelay, String cronSchedule) {
        if (scheduleType.equals("fixedDelay")) {
            log.info("          Repeat Mode : " + (repeat ? "Repeat on Fixed Delay" : "Run Once & Stop"));
            log.info("     Downloader Delay : " + downloaderDelay / 60000 + " min(s)");
        } else if (scheduleType.equals("cron")){
            log.info("  Repeat Mode : " + (repeat ? "Cron Schedule" : "Run Once & Stop"));
            log.info("Cron Schedule : " + cronSchedule);
        }
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
