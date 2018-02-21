package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.databind.JsonNode;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.awt.*;

/* List fetchPhotosReadyForDownload() {
        RestResponse response = restBuilder.get("$apiURL/api/photos?status=$READY_FOR_DOWNLOAD") {
            header "X-Auth-Token", accessToken
        }

        if (response.status != 200) {
            throw new Exception("Error polling for downloadable images: $response.text")
        }

        return response.json
    }*/

@SpringBootApplication
public class PhotoDownloaderApplication {


	public static void main(String[] args) {
		SpringApplication.run(PhotoDownloaderApplication.class, args);

		System.out.println("Hello World");
		//Unirest.shutdown();
	}

	List fetchPhotosReadyForDownload() {
		//HttpResponse<JsonNode> jsonResponse = Unirest.post(

		return null;
	}

}
