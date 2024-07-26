package com.cloudcard.photoDownloader

import groovy.json.JsonOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

import java.text.SimpleDateFormat


@Component
@ConditionalOnProperty(name = 'Origo.useOrigo', havingValue = 'true')
class OrigoEventLoggingService {
    // configures temporary local storage for Origo event ids and timestamps

    private static final Logger log = LoggerFactory.getLogger(OrigoEventLoggingService.class);

    static File _eventLogDirectory

    static List<File> _eventLogs

    static removeOldLogs() {
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
            _eventLogs.each {log.info("EVENT LOGS ARRAY: " + it.name)}

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

    static generateLog(List<Map> events) {
        // takes in list of event date:id objects, creates json file and writes log.
        // json file takes of the name of the oldest event in the log in milliseconds

        def sortedEvents = events.sort {
            it.date
        }

        def oldestEventDate = sortedEvents[0].date.toString()

        String fileName = isoToMilliseconds(oldestEventDate)

        String json = JsonOutput.toJson(sortedEvents)

        File eventLog = createLogFile("${_eventLogDirectory.getPath().toString()}", "${fileName}")

        if (eventLog.exists()) {
            eventLog.withWriter {
                writer -> writer.writeLine json
            }
        } else {
            log.error("Origo Error while creating log file")
        }

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
