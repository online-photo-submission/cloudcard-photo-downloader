package com.cloudcard.photoDownloader

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter


@Component
@ConditionalOnProperty(name = 'Origo.useOrigo', havingValue = 'true')
class OrigoEventLoggingService {
    // configures temporary local storage for Origo event ids and timestamps

    private static final Logger log = LoggerFactory.getLogger(OrigoEventLoggingService.class);

    static File _eventLogDirectory

    static List<File> _eventLogs // [0] is older than [1]

    static removeOldLogs() {
        // NEEDS REFACTORING
        // deletes 3rd oldest log file. keeps locally stored log files always at 2 or less.
        // currently only works with .json files OR 4-letter file extensions
        File[] files = _eventLogDirectory.listFiles()
        def fileNames

        if (files.size() >= 3) {
            fileNames = new ArrayList<long>()

            files.each {
                long parsed = it.name.substring(0, it.name.length() - 5) as long
                fileNames.add(parsed)
            }

            fileNames.sort {
                a, b -> b <=> a
            }

            fileNames = fileNames[0..1]

            ArrayList<File> filesToKeep = new ArrayList<File>()

            files.each {
                file ->
                    {
                        if (file.name != fileNames[0] + ".json" && file.name != fileNames[1] + ".json") {
                            log.info("**************************************")
                            log.info("DELETING OLD ORIGO EVENT LOGS: ${file.name}")
                            log.info("**************************************")
                            file.delete()
                        } else {
                            if (_eventLogs?.size() == 0 || _eventLogs == null) {
                                _eventLogs = new ArrayList<File>()
                            }

                            filesToKeep.add(file)
                        }
                    }
            }

            _eventLogs = filesToKeep

        }

    }

    static createLogDirectory() {
        // creates directory and log file if they don't exist

        File eventLogDirectory = new File("${System.getProperty("user.dir").toString()}/origo-event-logs")

        if (eventLogDirectory.exists() && eventLogDirectory.isDirectory()) {
            log.info('origo-event-logs DIRECTORY EXISTS.')
        } else {
            log.info('origo-event-logs DIRECTORY DOESN\'T EXIST: Creating directory.')
            eventLogDirectory.mkdir()
        }

        _eventLogDirectory = eventLogDirectory
    }

    static File createLogFile(String path, String fileName) {
        File eventLog = new File("${path}/${fileName}.json")

        eventLog.createNewFile()

        return eventLog
    }

    def getLastEvent() {}

    static processEvents(List<Map> events) {
        // uses event IDs to filter out old/processed events

        if (_eventLogs == null) {
            log.info("Reference to existing logs was null. Checking for Origo event log files... ...")
            _eventLogs = new File(_eventLogDirectory.toString()).listFiles()
        }

        if (_eventLogs.size() == 0) {
            return events
        } else {
            def jsonSlurper = new JsonSlurper()
            log.info("Processing incoming events ... ...")
            List<Map> newEvents = []
            File mostRecentLog = _eventLogs[1] ?: _eventLogs[0]
            String json = mostRecentLog.text
            def lastEvents = (List<Object>) jsonSlurper.parseText(json)
            def comparison = [:]

            lastEvents.each {
                event -> comparison[event.id] = true
            }

            for (Map event in events) {
                log.info(event.id.toString())
                if (!comparison[event.id]) {
                    newEvents.add(event)
                }
            }

            return newEvents
        }

    }

    static generateLog(List<Map> events) {
        // takes in list of event date:id objects, creates json file and writes log.
        // json file takes of the name of the oldest event in the log in milliseconds

        def sortedEvents = events.sort {
            it.date
        }

        String nowIso = nowAsIsoFormat() // Will need to be refactored to time of API call, not time of generating log

        String fileName = isoToMilliseconds(nowIso)

        String json = JsonOutput.toJson(sortedEvents)

        File eventLog = createLogFile("${_eventLogDirectory.getPath().toString()}", "${fileName}")

        log.info("Generating Origo event log.")
        if (eventLog.exists()) {
            eventLog.withWriter {
                writer -> writer.writeLine json
            }
        } else {
            log.error("Origo Error while creating log file")
        }

    }

    private static String nowAsIsoFormat() {
        Instant now = Instant.now()
        ZonedDateTime utc = now.atZone(ZoneId.of("UTC"))
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT
        String nowIso = formatter.format(utc)
        return nowIso
    }

    private static String isoToMilliseconds(String date) {
        def isoDateFormat = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss')
        isoDateFormat.setTimeZone(TimeZone.getTimeZone('UTC'))

        def fileName = isoDateFormat.parse(date).time.toString()
        return fileName
    }

    private static String millisecondsToIso(long milliseconds) {
        def isoDateFormat = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss')
        isoDateFormat.setTimeZone(TimeZone.getTimeZone('UTC'))

        def date = new Date(milliseconds)
        def isoDate = isoDateFormat.format(date)
        return isoDate
    }

    def getEventById() {}

}
