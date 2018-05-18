package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CloudCardPhotoService {

    private static final Logger log = LoggerFactory.getLogger(CloudCardPhotoService.class);
    public static final String READY_FOR_DOWNLOAD = "READY_FOR_DOWNLOAD";
    public static final String APPROVED = "APPROVED";
    public static final String DOWNLOADED = "DOWNLOADED";

    @Value("${cloudcard.api.url}")
    private String apiUrl;

    @Value("${cloudcard.api.accessToken}")
    private String accessToken;

    public CloudCardPhotoService() {

    }

    public CloudCardPhotoService(String apiUrl, String accessToken) {

        this.apiUrl = apiUrl;
        this.accessToken = accessToken;
    }


    public List<Photo> fetchApproved() throws Exception {

        return fetch(APPROVED);
    }

    public List<Photo> fetchReadyForDownload() throws Exception {

        return fetch(READY_FOR_DOWNLOAD);
    }

    public List<Photo> fetch(String status) throws Exception {

        HttpResponse<String> response = Unirest.get(apiUrl + "/photos?status=" + status).headers(standardHeaders()).asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when retrieving photo list to download.");
            return null;
        }

        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<Photo>>() {
        });
    }

    public Photo markAsDownloaded(Photo photo) throws Exception {

        return updateStatus(photo, DOWNLOADED);
    }

    public Photo updateStatus(Photo photo, String status) throws Exception {

        HttpResponse<String> response = Unirest.put(apiUrl + "/photos/" + photo.getId()).headers(standardHeaders()).body("{ \"status\": \"" + status + "\" }").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when updating photo.");
            return null;
        }

        return new ObjectMapper().readValue(response.getBody(), new TypeReference<Photo>() {
        });
    }

    private Map<String, String> standardHeaders() {

        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("X-Auth-Token", accessToken);
        return headers;
    }

}
