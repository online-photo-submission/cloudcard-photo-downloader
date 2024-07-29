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

    @Value('false')
    private boolean isAuthenticated

    @Autowired
    OrigoClient origoClient

    @Autowired
    OrigoEventLoggingService eventLoggingService

    @PostConstruct
    init() {

        List<Map> events = getEvents("", "") as List<Map>

        eventLoggingService.configure(events)

//        def processedEvents = eventLoggingService.processEvents(events)
//
//        eventLoggingService.generateLog(processedEvents)
//
//        eventLoggingService.removeOldLogs()
    }

//    def handleFilters() {
//         IF filters match filterSet THEN ping for Events using filterId  ELSE reconfigure filters to match filterSet property.
//        log.info("ORIGO: Checking for existing event filters ... ...")
//
//        def (result) = origoClient.listFilters()
//
//         if (/*result filters don't equal filterSet*/) {
//         delete old filter
//         create new filter
//         }
//
//         eventControlFlow()
//    }

    //def eventControlFlow() {
    // access logs for most recent event id and timestamp --> eventLoggingService
    // def events = getEvents(...)
    //}

    static List<Object> getEvents(String dateFrom, String dateTo = "", String filterId = "", String callbackStatus = "") {
//        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE
//
//        String dateFromISO = isoFormatter(dateFrom)
//        String dateToISO
//        if (!dateTo) {
//            dateToISO = eventLoggingService.nowAsIsoFormat()
//        } else {
//            dateToISO = isoFormatter(dateTo)
//        }
//
//        def (result) = origoClient.listEvents(dateFromISO, dateToISO, filterId, callbackStatus)

        List<Map> result = [
                [date: '2020-04-05T00:00:00', id: '1234'],
                [date: '2020-05-26T00:00:00', id: '7914'],
                [date: '2020-07-22T00:00:00', id: '3579'],
                [date: '2020-09-01T00:00:00', id: '6804'],
                [date: '2020-12-25T00:00:00', id: '8024'],
//                [date: '2021-03-25T00:00:00', id: '4682'],
//                [date: '2021-04-02T00:00:00', id: '7265'],
//                [date: '2021-06-11T00:00:00', id: '6803'],
//                [date: '2021-07-30T00:00:00', id: '1359'],
//                [date: '2021-08-18T00:00:00', id: '9135'],
//                [date: '2021-09-05T00:00:00', id: '3570'],
//                [date: '2021-10-14T00:00:00', id: '4680'],
//                [date: '2021-11-30T00:00:00', id: '2468'],
//                [date: '2022-01-10T00:00:00', id: '0246'],
//                [date: '2022-02-28T00:00:00', id: '5791'],
//                [date: '2022-03-13T00:00:00', id: '4681'],
//                [date: '2022-06-23T00:00:00', id: '7915'],
//                [date: '2022-08-07T00:00:00', id: '8675'],
//                [date: '2022-09-12T00:00:00', id: '1357'],
//                [date: '2022-10-16T00:00:00', id: '8025'],
//                [date: '2022-12-12T00:00:00', id: '2460'],
//                [date: '2023-01-15T00:00:00', id: '3456'],
//                [date: '2023-02-19T00:00:00', id: '9136'],
//                [date: '2023-03-17T00:00:00', id: '6802'],
//                [date: '2023-05-20T00:00:00', id: '7890'],
//                [date: '2023-07-21T00:00:00', id: '1358'],
//                [date: '2023-08-09T00:00:00', id: '3571'],
//                [date: '2023-11-29T00:00:00', id: '5792'],
//                [date: '2024-01-23T00:00:00', id: '0247'],
//                [date: '2024-04-07T00:00:00', id: '2469'],
//                [date: '2024-05-14T00:00:00', id: '5793'],
//                [date: '2024-06-09T00:00:00', id: '7913'],
//                [date: '2024-08-03T00:00:00', id: '6845']
        ]
        return result
    }

}
