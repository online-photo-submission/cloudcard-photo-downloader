package com.cloudcard.photoDownloader

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component


@Component
@ConditionalOnProperty(name = 'Origo.useOrigo', havingValue = 'true')
class OrigoEventLoggingService {
    // configures temporary local storage for Origo event ids and timestamps

    private static final Logger log = LoggerFactory.getLogger(OrigoEventLoggingService.class);

    static File _eventLog

    static configureLocalLogging() {
        // creates directory and log file if they don't exist

        File eventLogDirectory = new File(System.getProperty("user.dir").toString() + "/origo-event-log")

        if (eventLogDirectory.exists() && eventLogDirectory.isDirectory()) {
            log.info('origo-event-log DIRECTORY EXISTS')
        } else {
            log.info('origo-event-log DIRECTORY DOESN\'T EXIST: Creating directory with log.json')
            eventLogDirectory.mkdir()
        }

        boolean created = createJsonLog(eventLogDirectory.toString())

        log.info("${created ? "Origo log.json file created." : "Origo log.json file already exists."}")

    }

    static boolean createJsonLog(String path) {
        File eventLog = new File(path + "/log.json")

        boolean result = eventLog.createNewFile()

        _eventLog = eventLog

        return result
    }

    def getLastEvent() {}

    static writeEventToJson(String id, String timestamp) {
        if (_eventLog.exists()) {

            def event = [:]

            event[id] = timestamp

            String json = JsonOutput.toJson(event)

            _eventLog.withWriterAppend {
                writer -> writer.writeLine json
            }

        }


    }

    def getEventById() {}


}
