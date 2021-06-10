package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank;

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

    @Value("${downloader.putStatus:DOWNLOADED}")
    private String putStatus;

    @Value("${downloader.fetchStatuses:READY_FOR_DOWNLOAD}")
    private String[] fetchStatuses;

    public CloudCardPhotoService() {

    }

    public CloudCardPhotoService(String apiUrl, String accessToken) {

        this.apiUrl = apiUrl;
        this.accessToken = accessToken;
    }

    @PostConstruct
    void init() {

        throwIfBlank(apiUrl, "The CloudCard API URL must be specified.");
        throwIfBlank(accessToken, "The CloudCard API access token must be specified.");

        log.info("         Access Token : " + "..." + accessToken.substring(3, 8) + "...");
        log.info("              API URL : " + apiUrl);
        log.info("           PUT Status : " + putStatus);
        log.info("       Fetch Statuses : " + String.join(" , ", fetchStatuses));
    }

    public List<Photo> fetchApproved() throws Exception {

        return fetch(APPROVED);
    }

    public List<Photo> fetchReadyForDownload() throws Exception {

        return fetch(fetchStatuses);
    }

    public List<Photo> fetch(String[] statuses) throws Exception {

        List<Photo> photoList = new ArrayList<>();

        for (String status : statuses) {
            List<Photo> photos = fetch(status);
            photoList.addAll(photos);
        }

        return photoList;
    }

    public List<Photo> fetch(String status) throws Exception {

        String url = apiUrl + "/trucredential/" + accessToken + "/photos?status=" + status + "&base64EncodedImage=false&max=1000&additionalPhotos=true";
        HttpResponse<String> response = Unirest.get(url).headers(standardHeaders()).asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when retrieving photo list to download.");
            return new ArrayList<>();
        }

        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<Photo>>() {
        });
    }

    public Photo markAsDownloaded(Photo photo) throws Exception {

        return updateStatus(photo, putStatus);
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
