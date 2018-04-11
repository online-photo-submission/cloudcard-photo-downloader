package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.JsonParseException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.jvnet.hk2.annotations.Service;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

@SpringBootApplication
public class PhotoDownloaderApplication {


    private static final String API_URL = " https://api.onlinephotosubmission.com/api";


    public static void main(String[] args) {
		SpringApplication.run(PhotoDownloaderApplication.class, args);

        HttpResponse<JsonNode> myList = null;
        try {
            myList = fetchPhotosReadyForDownload();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        System.out.println(myList);

		//Unirest.shutdown();
	}



    private static HttpResponse<JsonNode> fetchPhotosReadyForDownload() throws UnirestException {
        Scanner sc = new Scanner(System.in);

        System.out.println("Enter Username: ");
        String username = "brian.akumah@gmail.com";

        System.out.println("Enter Password: ");             //TODO: Mask the user imputed password
        String password = "BBaller213&";

        String body = "{ \"username\" : \"" + username + "\", \"password\" : \"" + password + "\" }";
        System.out.println("body == " + body);
        HttpResponse<String> adminDesc = Unirest.post(API_URL + "/login")
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .body(body).asString();


        System.out.println(adminDesc.getBody());

        String accessToken = adminDesc.getBody();

        int Status = adminDesc.getStatus();

        System.out.println("STATUS IS -------  " + Status);



        System.out.println("This is the access token" + accessToken);

//        try {
//            HttpResponse<JsonNode> jsonImageResponse;
//            jsonImageResponse = Unirest.get(API_URL +"/photos[?status=READY_FOR_DOWNLOAD]")
//            .header("X-Auth-Token", accessToken).asJson();
//
//            return jsonImageResponse;
//
//        } catch (UnirestException e) {
//            e.printStackTrace();
//        }


        return null;
    }

}
