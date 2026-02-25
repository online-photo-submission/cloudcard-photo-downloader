package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

import javax.management.timer.Timer

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank
import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue

@Service
@ConditionalOnProperty(value = "downloader.photoService", havingValue = "CloudCardPhotoService")
class CloudCardPhotoService implements PhotoService {

    private static final Logger log = LoggerFactory.getLogger(CloudCardPhotoService.class)

    @Value('${downloader.putStatus:DOWNLOADED}')
    private String putStatus

    @Value('${downloader.fetchStatuses:READY_FOR_DOWNLOAD}')
    private String[] fetchStatuses

    @Autowired
    CloudCardClient cloudCardClient

    CloudCardPhotoService() {
    }

    @PostConstruct
    void init() {
        throwIfTrue(!cloudCardClient.isConfigured(), "The CloudCardClient must be configured when the CloudCardPhotoService is used.")

        log.info("           PUT Status : " + putStatus)
        log.info("       Fetch Statuses : " + String.join(" , ", fetchStatuses))
    }

    @Override
    long minDownloaderDelay() {
        Timer.ONE_MINUTE * 10
    }

    @Override
    List<Photo> fetchReadyForDownload() throws Exception {
        cloudCardClient.fetchWithBytes(fetchStatuses)
    }

    @Override
    Photo markAsDownloaded(Photo photo) throws Exception {
        cloudCardClient.updateStatus(photo, putStatus)
    }

    @Override
    Photo markAsFailed(Photo photo, String errorMessage) throws Exception {
        cloudCardClient.updateStatus(photo, CloudCardClient.ON_HOLD, errorMessage)
    }

    @Override
    void close() {
        cloudCardClient.close()
    }

}
