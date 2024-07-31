package com.cloudcard.photoDownloader

import org.springframework.stereotype.Component

import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Component
class Utils {
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

}
