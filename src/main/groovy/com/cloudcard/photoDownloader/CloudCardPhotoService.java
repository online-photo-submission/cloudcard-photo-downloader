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
    PreProcessor preProcessor;

    @Autowired
    CloudCardClient cloudCardClient;

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
        return cloudCardClient.fetchWithBytes(fetchStatuses);
    }

    @Override
    public Photo markAsDownloaded(Photo photo) throws Exception {
        return updateStatus(photo, putStatus);
    }

    public Photo updateStatus(Photo photo, String status) throws Exception {
        return cloudCardClient.updateStatus(photo, status);
    }

}
