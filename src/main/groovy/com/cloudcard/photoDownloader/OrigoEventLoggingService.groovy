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

    static removeOldLogs(File newLog) {
        // Removes all but the recently created log file
        File[] files = _eventLogDirectory.listFiles()

        if (files.size() >= 1) {

            files.each {
                if (it.name != newLog.name) {
                    it.delete()
                }
            }
        }

        _eventLogs = [newLog]

    }

    private static long parseLongFromFilename(File file) {
        long parsed = file.name.substring(0, file.name.length() - 5) as long
        parsed
    }

    static createLogDirectory() {
        // creates directory if it doesn't exist

        File eventLogDirectory = new File("${System.getProperty("user.dir").toString()}/origo-event-logs")

        try {
            if (eventLogDirectory.exists() && eventLogDirectory.isDirectory()) {
                log.info('origo-event-logs DIRECTORY EXISTS.')
            } else {
                log.info('origo-event-logs DIRECTORY DOESN\'T EXIST: Creating directory.')
                eventLogDirectory.mkdir()
            }
        } catch (Exception ex) {
            log.error(ex.message)
        }

        _eventLogDirectory = eventLogDirectory
    }

    static File createLogFile(String path, String fileName) {
        File eventLog = new File("${path}/${fileName}.json")

        eventLog.createNewFile()

        return eventLog
    }

    static configure(List<Object> events) {
        // Helper method makes conditional decisions based on state of existing logs or lack thereof
        createLogDirectory()

        if (_eventLogs == null) {
            // No reference to local logs in memory. Create reference.
            log.info("ORIGO: Reference to existing logs was null. Checking for Origo event log files.")
            _eventLogs = new File(_eventLogDirectory.toString()).listFiles()
        }

        if (/*_eventLogs.size() == 0*/false) {
            // Check for remote event logs
            log.info("ORIGO: No existing Origo event logs. Checking for remotely stored events.")
            // def remoteEvents = makeRequest() -> No remote records? Initiate cold start.
            // --> facts and logic
            // getNewEventsOnly(events, remoteEvents)

        } else {
            // There are existing local logs
            log.info("ORIGO: There are existing local logs. Checking logs for processing new events.")
            def newEvents = getNewEventsOnly(events)

            generateLog newEvents
        }
    }

    static List<Object> getNewEventsOnly(List<Object> incomingEvents, def comparisonEvents = null) {
        // uses event IDs, other properties to filter out old/processed events
        // FUTURE - might events take on properties depending on what has been done to them, different processing statuses?

        log.info("Checking if events are new ... ...")
        if (!_eventLogs[0]) createTestLog()
        def jsonSlurper = new JsonSlurper()
        List<Object> newEvents = new ArrayList<Object>()
        def comparison
        List<Object> lastEvents = []

        if (/*comparisonEvents*/false) {
            // Parse into Object
            //comparisonEvents.each {lastEvents.add(it.something)}
        } else {
            comparison = [:]
            File existingLog = _eventLogs[0]
            String json = existingLog.text
            lastEvents =  jsonSlurper.parseText(json) as List<Object>
        }

        lastEvents.each {
            event -> comparison[event.id as String] = true
        }

        for (Object event in incomingEvents) {
            log.info(event.id.toString())
            if (!comparison[event.id as String]) {
                newEvents.add(event)
            }
        }

        return (lastEvents + newEvents) as List<Object>
    }

    static generateLog(List<Object> events) {
        // takes in list of event date:id objects, creates json file and writes log.
        // json file takes of the name of the oldest event in the log in milliseconds

        def sortedEvents = events.sort {
            it.time
        }

        String nowIso = nowAsIsoFormat() // Will need to be refactored to time of API call, not time of generating log

        String fileName = isoToMilliseconds(nowIso)

        String json = JsonOutput.toJson(sortedEvents)

        File eventLog = createLogFile("${_eventLogDirectory.getPath().toString()}", "${fileName}")

        try {
            if (eventLog.exists()) {
                log.info("Generating Origo event log.")
                eventLog.withWriter {
                    writer -> writer.writeLine json
                }
            } else {
                log.error("Origo Error while creating log file")
            }
        } catch (Exception ex) {
            log.error(ex.message)
        }

        removeOldLogs(eventLog)
    }

    static String nowAsIsoFormat() {
        Instant now = Instant.now()
        ZonedDateTime utc = now.atZone(ZoneId.of("UTC"))
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT
        String nowIso = formatter.format(utc)
        return nowIso
    }

    static String isoToMilliseconds(String date) {
        def isoDateFormat = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss')
        isoDateFormat.setTimeZone(TimeZone.getTimeZone('UTC'))

        def fileName = isoDateFormat.parse(date).time.toString()
        return fileName
    }

    static String millisecondsToIso(long milliseconds) {
        def isoDateFormat = new SimpleDateFormat('yyyy-MM-dd\'T\'HH:mm:ss')
        isoDateFormat.setTimeZone(TimeZone.getTimeZone('UTC'))

        def date = new Date(milliseconds)
        def isoDate = isoDateFormat.format(date)
        return isoDate
    }

    static def createTestLog() {

        String resultJson2 = """ 
[
                  {
                    "type": "com.origo.mi.user",
                    "specversion": "1.0",
                    "source": "https://dev.portal.origo.hidcloud.com/credential-management/customer/1003233/user/6789",
                    "id": "k1l2m3n4-5678-9012-ab34-cd56ef789012",
                    "time": "2024-03-12T14:45:30.123Z",
                    "datacontenttype": "application/vnd.hidglobal.origo.events.user-2.0+json",
                    "data": {
                      "organization_id": "1003233",
                      "userId": "6789",
                      "firstName": "Olivia",
                      "lastName": "Martinez",
                      "status": "USER_CREATED",
                      "email": "olivia.martinez@example.com"
                    }
                  },
                  {
                    "type": "com.origo.mi.user",
                    "specversion": "1.0",
                    "source": "https://dev.portal.origo.hidcloud.com/credential-management/customer/1003233/user/2346",
                    "id": "l2m3n4o5-6789-0123-ab45-cd67ef890123",
                    "time": "2022-10-20T16:11:45.789Z",
                    "datacontenttype": "application/vnd.hidglobal.origo.events.user-2.0+json",
                    "data": {
                      "organization_id": "1003233",
                      "userId": "2346",
                      "firstName": "Liam",
                      "lastName": "Nguyen",
                      "status": "USER_CREATED",
                      "email": "liam.nguyen@example.com"
                    }
                  },
                  {
                    "type": "com.origo.mi.user",
                    "specversion": "1.0",
                    "source": "https://dev.portal.origo.hidcloud.com/credential-management/customer/1003233/user/7891",
                    "id": "m3n4o5p6-7890-1234-ab56-cd78ef901234",
                    "time": "2021-05-18T09:22:33.456Z",
                    "datacontenttype": "application/vnd.hidglobal.origo.events.user-2.0+json",
                    "data": {
                      "organization_id": "1003233",
                      "userId": "7891",
                      "firstName": "Ethan",
                      "lastName": "Williams",
                      "status": "USER_CREATED",
                      "email": "ethan.williams@example.com"
                    }
                  },
                  {
                    "type": "com.origo.mi.user",
                    "specversion": "1.0",
                    "source": "https://dev.portal.origo.hidcloud.com/credential-management/customer/1003233/user/7890",
                    "id": "b2c3d4e5-f678-9012-ab34-cd56ef789012",
                    "time": "2024-02-28T08:12:34.567Z",
                    "datacontenttype": "application/vnd.hidglobal.origo.events.user-2.0+json",
                    "data": {
                        "organization_id": "1003233",
                        "userId": "7890",
                        "firstName": "Jane",
                        "lastName": "Smith",
                        "status": "USER_CREATED",
                        "email": "jane.smith@example.com"
                    }
                }
]

"""
        JsonSlurper jsonSlurper = new JsonSlurper()
        def result = jsonSlurper.parseText(resultJson2) as List<Object>

        generateLog(result)
    }

}
