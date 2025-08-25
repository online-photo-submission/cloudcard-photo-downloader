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


@Component
class CloudCardClient{

    private static final Logger log = LoggerFactory.getLogger(CloudCardClient.class)
    public static final String READY_FOR_DOWNLOAD = "READY_FOR_DOWNLOAD"
    public static final String APPROVED = "APPROVED"
    public static final String DOWNLOADED = "DOWNLOADED"

    @Value('${cloudcard.api.url}')
    private String apiUrl

    @Autowired
    TokenService tokenService

    Photo updateStatus(Photo photo, String status, String message = null) throws Exception {

        String url = "${apiUrl}/photos/${photo.id}"

        if (message && status=='ON_HOLD') {
            String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.toString())
            url += "?onHoldReason=${encoded}"
        }

        HttpResponse<String> response = Unirest.put(url)
                .headers(standardHeaders())
                .body("{ \"status\": \"${status}\" }")
                .asString()

        if (response.status != 200) {
            log.error("Status ${response.status} returned from CloudCard API when updating photo: ${photo.id}")
            log.error("\t${response.body}")
            return null
        }

        return new ObjectMapper().readValue(response.body, new TypeReference<Photo>() {})
    }

    private Map<String, String> standardHeaders() {

        Map<String, String> headers = new HashMap<>()
        headers.put("accept", "application/json")
        headers.put("Content-Type", "application/json")
        headers.put("X-Auth-Token", tokenService.getAuthToken())
        return headers
    }

}