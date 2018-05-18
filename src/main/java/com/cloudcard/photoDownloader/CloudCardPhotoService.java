package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
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

        return fetch(status, true);
    }

    public List<Photo> fetch(String status, boolean fetchBytes) throws Exception {

        HttpResponse<String> response = Unirest.get(apiUrl + "/photos?status=" + status).header("accept", "application/json").header("X-Auth-Token", accessToken).header("Content-Type", "application/json").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + "returned from CloudCard API when retrieving photo list to download.");
            return null;
        }

        List<Photo> photos = new ObjectMapper().readValue(response.getBody(), new TypeReference<List<Photo>>() {
        });

        if (fetchBytes) {
            for (Photo photo : photos) {
                fetchBytes(photo);
            }
        }
        return photos;
    }


    private void fetchBytes(Photo photo) throws Exception {

        String bytesURL = photo.getLinks().getBytes();
        HttpResponse<String> response = Unirest.get(bytesURL).header("accept", "image/jpeg;charset=utf-8").header("Content-Type", "image/jpeg;charset=utf-8").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + "returned from CloudCard API when retrieving photos bytes.");
            return;
        }

        photo.setBytes(getBytes(response));
    }

    private byte[] getBytes(HttpResponse<String> response) throws IOException {

        InputStream rawBody = response.getRawBody();
        byte[] bytes = new byte[ rawBody.available() ];
        rawBody.read(bytes);
        return bytes;
    }

}
