package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class OrigoService {
    // Handles business logic / processing for incoming events and requests from API
    private static final Logger log = LoggerFactory.getLogger(OrigoService.class);

    @Value('${Origo.filterSet')
    String filterSet

    @Autowired
    OrigoClient client

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
//        handleExistingSubscriptions()
    }

    def handleExistingSubscriptions() {
        def ( result ) = client.listCallbackSubscriptions()

        log.info("Callback subscriptions: $result.json")

    }
}
