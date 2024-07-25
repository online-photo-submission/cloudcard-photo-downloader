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

    @Value('false')
    private boolean isAuthenticated

    @PostConstruct
    init() {
        // authenticate()
//        if (isAuthenticated) {
//            // do something
//        } else {
//            log.error('********** Origo services cannot be authenticated **********')
//        }
    }

    def handleFilters() {
        // IF filters match filterSet THEN ping for Events using filterId  ELSE reconfigure filters to match filterSet property.
        log.info("ORIGO: Checking for existing event filters ... ...")

        def (result) = origoClient.listFilters()

//        log.info(result)

    }


    def getEvents() {
        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE

        def (result) = origoClient.listEvents()
    }

}
