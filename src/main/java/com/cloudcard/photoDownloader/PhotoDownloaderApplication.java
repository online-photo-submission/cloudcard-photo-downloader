package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class PhotoDownloaderApplication {

    private static final String API_URL = " https://api.onlinephotosubmission.com/api";

    public static void main(String[] args) throws UnirestException {

        SpringApplication.run(PhotoDownloaderApplication.class, args);

        // calls fetchPhotosReadyForDownload and assigns it to photos
        List<User> photoList = null;

        photoList = fetchPhotosReadyForDownload();

        //Prints out list of users
        assert photoList != null;
        for (User aPhotoList : photoList) {
            System.out.println("\n\n\n\n\nStudent ID: " + aPhotoList.getId());
            System.out.println("Student Username: " + aPhotoList.getPerson().getUsername());
            System.out.println("Bytes: " + aPhotoList.getLinks().getBytes());
            System.out.println("Public Key: " + aPhotoList.getPublicKey() + "\n");

            fetchPhotoBytes(aPhotoList.getLinks().getBytes(), aPhotoList.getId());
        }

    }

    private static List<User> fetchPhotosReadyForDownload() throws UnirestException {

        String username = "brian.akumah@gmail.com";
        String password = "BBaller213&";

        if (username == null || username == "") {
            Scanner login = new Scanner(System.in);
            System.out.println("Enter Username: ");
            username = login.nextLine();
            System.out.println("Enter Password: ");                 //TODO: Mask the user imputed password
            password = login.nextLine();
        }

        if ((password == null || password == "") && (username != null || username == "")) {
            Scanner login = new Scanner(System.in);
            System.out.println("Enter Password: ");
            password = login.nextLine();
        }

        String loginJSON = "{ \"username\" : \"" + username + "\", \"password\" : \"" + password + "\" }";
        HttpResponse<String> adminResponse = Unirest.post(API_URL + "/login").header("accept", "application/json").header("Content-Type", "application/json").body(loginJSON).asString();

        if (adminResponse.getStatus() != 200) {
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


        HttpResponse<String> userResponse = Unirest.get(API_URL + "/photos?status=READY_FOR_DOWNLOAD").header("accept", "application/json").header("X-Auth-Token", adminAccessToken).header("Content-Type", "application/json").asString();

        String userResponseBody = userResponse.getBody();


        if (userResponse.getStatus() != 200) {
            System.out.println("***Error " + userResponse.getStatus() + ":  There is a problem receiving the user JSON");
        }

        List<User> userResponseJSON = null;
        try {
            userResponseJSON = new ObjectMapper().readValue(userResponseBody, new TypeReference<List<User>>() {
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return userResponseJSON;
    }

    private static byte[] fetchPhotoBytes(String bytesURL, Integer uId) throws UnirestException {

        //Recieves the list of photos ready for download
        HttpResponse<String> httpResponse = Unirest.get(bytesURL).header("accept", "image/jpeg;charset=utf-8").header("Content-Type", "image/jpeg;charset=utf-8").asString();

        System.out.println("******STATUS******" + httpResponse.getStatus() + "\n");
        System.out.println(httpResponse.getBody());


        try {
            InputStream rawBody = httpResponse.getRawBody();
            byte[] bytes = new byte[ rawBody.available() ];
            rawBody.read(bytes);

            File file = new File("./" + uId + ".jpeg");

            FileOutputStream outputStream = null;
            try {
                outputStream = new FileOutputStream(file);

                if (!file.exists()) {
                    file.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                outputStream.write(bytes);
                outputStream.flush();
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
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
