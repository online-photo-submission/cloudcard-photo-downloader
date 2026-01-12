package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

/**
 * Stub Client for HFC (HID Fargo Connect) integration.
 * HFC will implement the actual API integration.
 */
@Component
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "HfcStorageService")
class HfcClient {

    static final Logger log = LoggerFactory.getLogger(HfcClient.class)

    @Value('${HfcClient.apiUrl}')
    String apiUrl

    @PostConstruct
    void init() {
        throwIfBlank(apiUrl, "The HFC API URL must be specified.")
        log.info("              HFC API URL : $apiUrl")
        log.warn("HfcClient is a stub - HFC will implement API integration")
    }

    boolean uploadPhoto(Photo photo) {
        log.warn("HFC photo upload not implemented")
        return false
    }
}
