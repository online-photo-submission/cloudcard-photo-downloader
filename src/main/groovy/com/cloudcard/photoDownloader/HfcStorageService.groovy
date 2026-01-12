package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue

/**
 * Stub Service for HFC (HID Fargo Connect) integration.
 * HFC will implement the actual API integration.
 */
@Service
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "HfcStorageService")
class HfcStorageService implements StorageService {

    static final Logger log = LoggerFactory.getLogger(HfcStorageService.class)

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    FileNameResolver fileNameResolver

    @Autowired
    HfcClient hfcClient

    @PostConstruct
    void init() {
        throwIfTrue(fileNameResolver == null, "The File Name Resolver must be specified.")
        log.info("   File Name Resolver : $fileNameResolver.class.simpleName")
        log.warn("HfcStorageService is a stub - HFC will implement photo upload")
    }

    List<PhotoFile> save(Collection<Photo> photos) {
        log.warn("HFC photo upload not implemented")
        return []
    }

    PhotoFile save(Photo photo) {
        log.warn("HFC photo upload not implemented")
        return null
    }
}
