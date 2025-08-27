package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.management.timer.Timer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank;

@Service
@ConditionalOnProperty(value = "downloader.photoService", havingValue = "CloudCardPhotoService")
public class CloudCardPhotoService implements PhotoService {

    private static final Logger log = LoggerFactory.getLogger(CloudCardPhotoService.class);
    public static final String READY_FOR_DOWNLOAD = "READY_FOR_DOWNLOAD";
    public static final String APPROVED = "APPROVED";
    public static final String DOWNLOADED = "DOWNLOADED";

    @Value("${cloudcard.api.url}")
    private String apiUrl;

    @Value("${downloader.putStatus:DOWNLOADED}")
    private String putStatus;

    @Value("${downloader.fetchStatuses:READY_FOR_DOWNLOAD}")
    private String[] fetchStatuses;

    @Autowired
    RestService restService;

    @Autowired
    PreProcessor preProcessor;

    @Autowired
    TokenService tokenService;

    public CloudCardPhotoService() {

    }

    public CloudCardPhotoService(String apiUrl) {

        this.apiUrl = apiUrl;
    }

    @PostConstruct
    void init() {

        throwIfBlank(apiUrl, "The CloudCard API URL must be specified.");

        log.info("              API URL : " + apiUrl);
        log.info("           PUT Status : " + putStatus);
        log.info("       Fetch Statuses : " + String.join(" , ", fetchStatuses));
        log.info("        Pre-Processor : " + preProcessor.getClass().getSimpleName());
    }

    @Override
    public long minDownloaderDelay() {
        return Timer.ONE_MINUTE * 10;
    }

    @Override
    public List<Photo> fetchReadyForDownload() throws Exception {

        List<Photo> photos = fetch(fetchStatuses);
        for (Photo photo : photos) {
            Photo processedPhoto = preProcessor.process(photo);
            restService.fetchBytes(processedPhoto);
        }
        return photos;
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

        String url = apiUrl + "/trucredential/" + tokenService.getAuthToken() + "/photos?status=" + status + "&base64EncodedImage=false&max=1000&additionalPhotos=true";
        HttpResponse<String> response = Unirest.get(url).headers(standardHeaders()).asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when retrieving photo list to download.");
            return new ArrayList<>();
        }

        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<Photo>>() {
        });
    }
    //TODO:Refactor to use cloudcardClient where it makes sense
    @Override
    public Photo markAsDownloaded(PhotoFile photo) throws Exception {

        return updateStatus(photo, putStatus);
    }

    public Photo updateStatus(PhotoFile photoFile, String status) throws Exception {

        Photo photo = new Photo(photoFile.getPhotoId());

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
