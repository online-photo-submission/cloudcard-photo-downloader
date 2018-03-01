package com.cloudcard.photoDownloader;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Scanner;

@SpringBootApplication
public class PhotoDownloaderApplication {


    public static final String API_URL = "https://app.onlinephotosubmission.com/api";

    public static void main(String[] args) {
		SpringApplication.run(PhotoDownloaderApplication.class, args);

        HttpResponse<JsonNode> myList;
        myList = fetchPhotosReadyForDownload();

        System.out.println(myList);

        System.out.println("Hello World");
		//Unirest.shutdown();
	}



    private static HttpResponse<JsonNode> fetchPhotosReadyForDownload() throws UnirestException {
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter Username: ");
        String Username = sc.nextLine();

        System.out.println("Enter Password: ");
        String Password = sc.nextLine();


        HttpResponse<JsonNode> adminDesc = Unirest.post(API_URL + "/login")
                .body("{username:" + Username + ", password: " + Password + "}").asJson();
               /* .body({"username":Username, "password":Password}).asJson(); */

        String accessToken = adminDesc.getBody().toString();

        try {
            HttpResponse<JsonNode> jsonImageResponse = Unirest.post(API_URL +"/photos?status=READY_FOR_DOWNLOAD")
            .header("accept", "application/json").header("X-Auth-Token", accessToken).asJson();

            return jsonImageResponse;
        } catch (UnirestException e) {
            e.printStackTrace();
        }


        return null;
    }

}
