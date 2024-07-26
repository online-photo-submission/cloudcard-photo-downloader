package com.cloudcard.photoDownloader

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

import javax.swing.text.DateFormatter
import java.text.SimpleDateFormat


@Component
@ConditionalOnProperty(name = 'Origo.useOrigo', havingValue = 'true')
class OrigoEventLoggingService {
    // configures temporary local storage for Origo event ids and timestamps

    private static final Logger log = LoggerFactory.getLogger(OrigoEventLoggingService.class);

    static File _eventLogDirectory

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

        _eventLogDirectory = eventLogDirectory
    }

    static boolean createJsonLog(String path, String fileName) {
        File eventLog = new File("${path}/${fileName}.json")

        boolean result = eventLog.createNewFile()

        _eventLog = eventLog

        return result
    }

    def getLastEvent() {}

    static writeEventsToJson(List<Map> events) {

        def sortedEvents = events.sort {
            it.date
        }

        def isoDateFormat = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss')
        isoDateFormat.setTimeZone(TimeZone.getTimeZone('UTC'))

        def fileName = isoDateFormat.parse(sortedEvents[0].date.toString()).time

        log.info("$fileName")

        String json = JsonOutput.toJson(sortedEvents)

        boolean created = createJsonLog("${_eventLogDirectory.getPath().toString()}", "${fileName}")

        if (created) {
            _eventLog.withWriter {
                writer -> writer.writeLine json
            }
        } else {
            log.error("Origo Error while creating log file")
        }

    }

    def getEventById() {}

}
