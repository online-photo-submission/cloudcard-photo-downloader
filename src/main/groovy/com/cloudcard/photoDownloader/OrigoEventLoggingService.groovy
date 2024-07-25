package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

import java.nio.file.Files

@Component
@ConditionalOnProperty(name = 'Origo.useOrigo', havingValue = 'true')
class OrigoEventLoggingService {
    // configures temporary local storage for Origo event ids and timestamps

    private static final Logger log = LoggerFactory.getLogger(OrigoEventLoggingService.class);

    static configureLocalLogging() {
        // creates directory and log file if they don't exist

        File eventLogDirectory = new File(System.getProperty("user.dir").toString() + "/origo-event-log")

        if (eventLogDirectory.exists() && eventLogDirectory.isDirectory()) {
            log.info('origo-event-log DIRECTORY EXISTS')
        } else {
            log.info('origo-event-log DIRECTORY DOESN\'T EXIST: Creating directory with log.json')
            eventLogDirectory.mkdir()
        }

        boolean created = createLog(eventLogDirectory.toString())

        log.info("${created ? "Origo log.json file created." : "Origo log.json file already exists."}")

    }

    static boolean createLog(String path) {
        File eventLog = new File(path + "/log.json")
        return eventLog.createNewFile()
    }

    def getLastEvent() {}

    def writeNewEvent() {

    }

    def getEventById() {}


}
