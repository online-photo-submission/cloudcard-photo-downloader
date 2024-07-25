package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

import java.time.format.DateTimeFormatter

@Service
@ConditionalOnProperty(name = 'Origo.useOrigo', havingValue = 'true')
class OrigoService {
    // Handles business logic / processing for incoming events and requests from API
    private static final Logger log = LoggerFactory.getLogger(OrigoService.class);

    @Value('${Origo.filterSet')
    String filterSet

    @Autowired
    OrigoClient origoClient

    @Autowired
    OrigoEventLoggingService eventLoggingService

    @Value('false')
    private boolean isAuthenticated

    @PostConstruct
    init() {
        //Check Storage for last timestamped event.
        eventLoggingService.configureLocalLogging()

        handleFilters()
    }

    def handleFilters() {
        // IF filters match filterSet THEN ping for Events using filterId  ELSE reconfigure filters to match filterSet property.
        log.info("ORIGO: Checking for existing event filters ... ...")

        def (result) = origoClient.listFilters()

//        if (/*result filters don't equal filterSet*/) {
            // delete old filter
            // create new filter
//        }

        // eventControlFlow()

    }

    def eventControlFlow() {
        // access logs for most recent event id and timestamp --> eventLoggingService
        // def events = getEvents(...)
    }

//    def getEvents(String dateFrom, String dateTo, String filterId = "", String callbackStatus = "") {
//        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE
//
//        String dateFromISO = isoFormatter(dateFrom)
//        String dateToISO = isoFormatter(dateTo)
//
//        def (result) = origoClient.listEvents(dateFromISO, dateToISO, filterId, callbackStatus)
//    }

}
