package com.cloudcard.photoDownloader;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class PhotoDownloaderApplication {

    private static final String API_URL = "https://api.onlinephotosubmission.com/api";

    public static void main(String[] args) throws Exception {

        SpringApplication.run(PhotoDownloaderApplication.class, args);

    }

    private static void markPhotosAsDownloaded(Integer uID) {

        try {
            HttpResponse<String> userResponse = Unirest.get(API_URL + "/photos?status=READY_FOR_DOWNLOAD").header("accept", "application/json")
                //  .header("X-Auth-Token", adminAccessToken)
                .header("Content-Type", "application/json").asString();
        } catch (UnirestException e) {
            e.printStackTrace();
        }


    }
}
