package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CloudCardClient{

    private static final Logger log = LoggerFactory.getLogger(CloudCardPhotoService.class);
    public static final String READY_FOR_DOWNLOAD = "READY_FOR_DOWNLOAD";
    public static final String APPROVED = "APPROVED";
    public static final String DOWNLOADED = "DOWNLOADED";

    @Value("${cloudcard.api.url}")
    private String apiUrl;

    @Value("${downloader.putStatus:DOWNLOADED}")
    private String putStatus;

    @Autowired
    TokenService tokenService;

    public Photo updateStatus(Photo photo, String status) throws Exception {

        HttpResponse<String> response = Unirest.put(apiUrl + "/photos/" + photo.getId()).headers(standardHeaders()).body("{ \"status\": \"" + status + "\" }").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when updating photo: " + photo.getId());
            log.error("\t" + response.getBody());
            return null;
        }

        return new ObjectMapper().readValue(response.getBody(), new TypeReference<Photo>() {
        });
    }

    private Map<String, String> standardHeaders() {

        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("X-Auth-Token", tokenService.getAuthToken());
        return headers;
    }

}