package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Service
public class DownloaderService {

    private static final Logger log = LoggerFactory.getLogger(DownloaderService.class);

    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");

    @Value("${cloudcard.api.url}")
    private String apiUrl;

    @Value("${cloudcard.api.accessToken}")
    private String accessToken;

    //TODO: Validate that it inclues a trailing /
    @Value("${downloader.photoDirectory}")
    String photoDirectory;

    @Scheduled(fixedDelayString = "${downloader.delay.milliseconds}")
    public void downloadPhotos() throws Exception {

        log.info("Downloading photos at " + dateFormat.format(new Date()));

        // calls fetchPhotosReadyForDownload and assigns it to photos
        List<Photo> photoList = fetchPhotosReadyForDownload();

        //Prints out list of users
        if (photoList != null) {
            for (Photo photo : photoList) {
                log.info("Downloading: " + photo.getPublicKey());
                fetchPhotoBytes(photo.getLinks().getBytes(), photo.getId());
            }
        } else {
            log.info("No photos found at " + dateFormat.format(new Date()));
        }
    }

    public List<Photo> fetchPhotosReadyForDownload() throws Exception {

        HttpResponse<String> response = Unirest.get(apiUrl + "/photos?status=READY_FOR_DOWNLOAD").header("accept", "application/json").header("X-Auth-Token", accessToken).header("Content-Type", "application/json").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + "returned from CloudCard API when retrieving photo list to download.");
            return null;
        }

        return new ObjectMapper().readValue(response.getBody(), new TypeReference<List<Photo>>() {
        });
    }

    public void fetchPhotoBytes(String bytesURL, Integer uId) throws Exception {

        HttpResponse<String> response = Unirest.get(bytesURL).header("accept", "image/jpeg;charset=utf-8").header("Content-Type", "image/jpeg;charset=utf-8").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + "returned from CloudCard API when retrieving photos bytes.");
            return;
        }

        String fileName = photoDirectory + "/" + uId + ".jpg";
        writeBytesToFile(fileName, getBytes(response));
    }

    private void writeBytesToFile(String fileName, byte[] bytes) throws IOException {

        File file = new File(fileName);

        FileOutputStream outputStream = new FileOutputStream(file);

        if (!file.exists()) {
            file.createNewFile();
        }
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }


    private byte[] getBytes(HttpResponse<String> response) throws IOException {

        InputStream rawBody = response.getRawBody();
        byte[] bytes = new byte[ rawBody.available() ];
        rawBody.read(bytes);
        return bytes;
    }

}
