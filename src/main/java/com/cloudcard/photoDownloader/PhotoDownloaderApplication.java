package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

@SpringBootApplication
public class PhotoDownloaderApplication {

    private static final String API_URL = " https://api.onlinephotosubmission.com/api";

    public static void main(String[] args) {
		SpringApplication.run(PhotoDownloaderApplication.class, args);

        List photoList = null;
        try {
            photoList = fetchPhotosReadyForDownload();
        } catch (UnirestException e) {
            e.printStackTrace();
        }

        //System.out.println();
	}

    private static List fetchPhotosReadyForDownload() throws UnirestException {
        Scanner sc = new Scanner(System.in);

        //System.out.println("Enter Username: ");
        String username = "";

        //System.out.println("Enter Password: ");             //TODO: Mask the user imputed password
        String password = "";

        if(username == null || username == ""){
            Scanner login = new Scanner(System.in);
            System.out.println("Enter Username: ");
            username = login.nextLine();
            System.out.println("Enter Password: ");
            password = login.nextLine();
        }

        if((password == null || password == "") && (username != null || username == "")){
            Scanner login = new Scanner(System.in);
            System.out.println("Enter Password: ");
            password = login.nextLine();
        }

        String loginJSON = "{ \"username\" : \"" + username + "\", \"password\" : \"" + password + "\" }";
        HttpResponse<String> adminResponse = Unirest.post(API_URL + "/login")
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .body(loginJSON).asString();

        if (adminResponse.getStatus() != 200){
            System.out.println("***Error " + adminResponse.getStatus() + ":  There is a problem Logging into Cloudcard");
        }

        String adminResponseBody = adminResponse.getBody();

        Administrator adminResponseJSON = null;
        try {
            adminResponseJSON = new ObjectMapper().readValue(adminResponseBody, Administrator.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String adminAccessToken = null;
        if (adminResponseJSON != null) {
            adminAccessToken = adminResponseJSON.getAccessToken();
        } else {
            System.out.println("***ERROR: No Access Token Found");
        }
        System.out.println("This is the access token: " + adminAccessToken);


            HttpResponse<String> userResponse = Unirest.get(API_URL +"/photos?status=READY_FOR_DOWNLOAD")
                    .header("accept", "application/json")
                    .header("X-Auth-Token", adminAccessToken)
                    .header("Content-Type", "application/json").asString();

        String userResponseBody = userResponse.getBody();

        System.out.println(userResponse.getStatus());
        System.out.println(userResponseBody);


        JSONArray userResponseArray = new JSONArray(userResponseBody);
        System.out.println(userResponseArray.length());

        List<User> userResponseJSON = null;
        try {
            userResponseJSON = new ObjectMapper().readValue(userResponseBody, new TypeReference<List<User>>(){});
        } catch (IOException e) {
            e.printStackTrace();
        }

//        for (User aStudentInfoJSON : userResponseJSON) {
//            System.out.println(aStudentInfoJSON.getPerson().getUsername());
//            System.out.println(aStudentInfoJSON.getLinks().getBytes());
//        }

        return userResponseJSON;
    }

}
