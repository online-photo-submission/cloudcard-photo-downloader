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

    static def doesLocalLogDirectoryExist() {
        File eventLogDirectory = new File(System.getProperty("user.dir").toString() + "/origo-event-log")


        if (eventLogDirectory.exists() && eventLogDirectory.isDirectory()) {
            log.info('origo-event-log DIRECTORY EXISTS')
            createLog(eventLogDirectory.toString())

        } else {
            log.info('origo-event-log DIRECTORY DOESN\'T EXIST: Creating directory with log.json')
            eventLogDirectory.mkdir()
            createLog(eventLogDirectory.toString())
        }
    }

    static def createLog(String path) {
        File eventLog = new File(path + "/log.json")
        eventLog.createNewFile()

    }


}
