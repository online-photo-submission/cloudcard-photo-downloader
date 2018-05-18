package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CloudCardPhotoService {

    private static final Logger log = LoggerFactory.getLogger(CloudCardPhotoService.class);
    public static final String READY_FOR_DOWNLOAD = "READY_FOR_DOWNLOAD";
    public static final String APPROVED = "APPROVED";

    @Value("${cloudcard.api.url}")
    private String apiUrl;

    @Value("${cloudcard.api.accessToken}")
    private String accessToken;

    public List<Photo> fetchApprovedPhotos() throws Exception {

        return fetchPhotos(APPROVED);
    }

    public List<Photo> fetchPhotosReadyForDownload() throws Exception {

        return fetchPhotos(READY_FOR_DOWNLOAD);
    }

    public List<Photo> fetchPhotos(String status) throws Exception {

        HttpResponse<String> response = Unirest.get(apiUrl + "/photos?status=" + status).header("accept", "application/json").header("X-Auth-Token", accessToken).header("Content-Type", "application/json").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + "returned from CloudCard API when retrieving photo list to download.");
            return null;
        }

        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<Photo>>() {
        });
    }

}
