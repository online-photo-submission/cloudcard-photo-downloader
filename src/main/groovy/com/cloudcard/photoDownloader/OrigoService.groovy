package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = 'Origo.useOrigo', havingValue = 'true')
class OrigoService {
    // Handles business logic / processing for incoming events and requests from API
    private static final Logger log = LoggerFactory.getLogger(OrigoService.class)

    @Value('${Origo.filterSet}')
    String filterSet

    @Autowired
    OrigoClient origoClient

    @Autowired
    OrigoEventStorageServiceLocal eventStorageServiceLocal

    @PostConstruct
    init() {
        if (!origoClient.isAuthenticated) {
            getNewAccessToken()
        }

        if (origoClient.isAuthenticated) {
            def events = getEvents()
        } else {
            log.info("ORIGO NOT AUTHENTICATED")
        }

    }

    void getNewAccessToken() {
        OrigoResponse response = origoClient.authenticate()

        if (response.success) {
            String token = response.body.access_token
            origoClient.setAccessToken(token)
        } else {
            log.error("ORIGO: Cannot obtain access token")
        }
    }

//    @Scheduled(fixedDelayString = '${downloader.delay.milliseconds}', initialDelayString = "5000")
    void getEvents(String dateFrom = "", String dateTo = "", String filterId = "", String callbackStatus = "") {
        OrigoResponse response = origoClient.listEvents(dateFrom, dateTo, filterId, callbackStatus)

        if (response.success) {
            ArrayList<Object> events = response.body as ArrayList<Object>

            processEvents(events)
        }
    }

    static void processEvents(ArrayList<Object> events) {
        log.info("ORIGOSERVICE: Processing received events.")
        events.each {
            if (it.data.status == "USER_CREATED") {
                log.info("ORIGOSERVICE: Provisioning Origo user, $it.data.userId, in CloudCard API")
                log.info "************************************************************"
            }
        }
    }
}
