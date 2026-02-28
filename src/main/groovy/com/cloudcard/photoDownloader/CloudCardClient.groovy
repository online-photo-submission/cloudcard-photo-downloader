package com.cloudcard.photoDownloader

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kong.unirest.core.HttpResponse
import kong.unirest.core.Unirest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets

import jakarta.annotation.PostConstruct

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank


@Component
class CloudCardClient {

    private static final Logger log = LoggerFactory.getLogger(CloudCardClient.class)
    public static final String READY_FOR_DOWNLOAD = "READY_FOR_DOWNLOAD"
    public static final String APPROVED = "APPROVED"
    public static final String DOWNLOADED = "DOWNLOADED"
    public static final String ON_HOLD = "ON_HOLD"
    public static final String FAILED = "FAILED"

    @Value('${cloudcard.api.url}')
    private String apiUrl

    @Autowired
    TokenService tokenService

    //TODO move the contents of this restService back into the cloudcard client.
    @Autowired
    RestService restService

    @Autowired
    PreProcessor preProcessor

    @PostConstruct
    void init() {
        log.info("              API URL : " + apiUrl)
        log.info("        Pre-Processor : " + preProcessor.getClass().getSimpleName())
    }

    /**
     * Each usage of the CloudCardClient should check isConfigured to make sure the client is configured.
     *
     * @return
     */
    boolean isConfigured() {
        return apiUrl && tokenService.isConfigured()
    }

    Photo updateStatus(Photo photo, String status, String message = null) throws Exception {
        String url = "${apiUrl}/photos/${photo.id}"

        if (message) {
            String encodedMessage = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())

            if (status == ON_HOLD) url += "?onHoldReason=${encodedMessage}"
            if (status == FAILED) url += "?failedReason=${encodedMessage}"
        }

        HttpResponse<String> response = Unirest.put(url)
            .headers(standardHeaders())
            .body("{ \"status\": \"${status}\" }")
            .asString()

        if (response.status != 200) {
            log.error("Status ${response.status} returned from CloudCard API when updating photo: ${photo.id}")
            return null
        }

        return new ObjectMapper().readValue(response.body, new TypeReference<Photo>() {})
    }

    List<Photo> fetchWithBytes(String[] fetchStatuses) throws Exception {
        List<Photo> photos = fetch(fetchStatuses)

        for (Photo photo : photos) {
            Photo processedPhoto = preProcessor.process(photo)
            restService.fetchBytes(processedPhoto)
        }

        return photos
    }

    List<Photo> fetch(String[] statuses) throws Exception {
        List<Photo> photoList = []

        for (String status : statuses) {
            List<Photo> photos = fetch(status)
            photoList.addAll(photos)
        }

        return photoList
    }

    List<Photo> fetch(String status) throws Exception {
        String url = "$apiUrl/trucredential/${tokenService.getAuthToken()}/photos?status=$status&base64EncodedImage=false&max=1000&additionalPhotos=true"
        HttpResponse<String> response = Unirest.get(url).headers(standardHeaders()).asString()

        if (response.getStatus() != 200) {
            log.error("Status $response.status returned from CloudCard API when retrieving photo list to download.")
            return []
        }

        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<Photo>>() {
        })
    }

    void close() {
        tokenService.logout()
    }

    private Map<String, String> standardHeaders() {
        [
            accept: "application/json",
            "Content-Type": "application/json",
            "X-Auth-Token": tokenService.getAuthToken()
        ]
    }

}