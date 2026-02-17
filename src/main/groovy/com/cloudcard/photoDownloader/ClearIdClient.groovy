package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

@Component
@ConditionalOnProperty(value = "IntegrationStorageService.client", havingValue = "ClearIdClient")
class ClearIdClient implements IntegrationStorageClient {

    static final Logger log = LoggerFactory.getLogger(ClearIdClient.class)

    @Value('${ClearIdClient.apiUrl:http://localhost:4010/api/v4}')
    String apiUrl

    @Value('${ClearIdClient.accountId:customer}')
    String accountId

    @Value('${ClearIdClient.clientId:user}')
    String clientId

    @Value('${ClearIdClient.clientSecret:secret}')
    String clientSecret

    @PostConstruct
    void init() {
        throwIfBlank(apiUrl, "The ClearId API URL must be specified.")
        throwIfBlank(accountId, "The ClearId accountId must be specified")
        throwIfBlank(clientId, "The ClearId clientId must be specified")
        throwIfBlank(clientSecret, "The ClearId clientSecret must be specified")

        log.info("     ClearId API URL : $apiUrl")
        log.info("   ClearId accountId : $accountId")
        log.info("    ClearId clientId : $clientId")
        log.info("ClearId clientSecret : ${clientSecret.length() > 0 ? "......" : ""}")
    }

    @Override
    String getSystemName() {
        return "ClearId"
    }

//    TODO: Add resiliency to at least handle rate limiting.
    @Override
    void putPhoto(String identifier, String photoBase64) {
        throw new FailedPhotoFileException("ClearID does not have a worker with accountId: $identifier. Please try again later. Thank you for your patience.")
    }

    @Override
    void close() {

    }

}
