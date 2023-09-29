package com.cloudcard.photoDownloader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Application {

    private static final String API_URL = "https://api.onlinephotosubmission.com/api";

    public static void main(String[] args) throws Exception {

        SpringApplication.run(Application.class, args);

    }
}
