package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

public class DatabaseStorageServiceTest {

    DatabaseStorageService databaseStorageService;
    CloudCardPhotoService cloudCardPhotoService;

    @Before
    public void Setup() {
        databaseStorageService = new DatabaseStorageService();
        cloudCardPhotoService = new CloudCardPhotoService("https://api.onlinephotosubmission.com/api", "egn880in41skuqqnoc7a11rf9gg268at");
    }

    @Test
    public void save_to_database() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.jdbc.Driver");
        dataSource.setUrl("jdbc:mysql://159.65.177.252:3306/photo_downloader_test");
        dataSource.setUsername("john");
        dataSource.setPassword("5646643ea");

        Person person = new Person();
        person.setIdentifier("derp");
        Photo photo = new Photo();
        photo.setPerson(person);

        List<Photo> photos = new ArrayList<>();
        photos.add(photo);

        try {
            databaseStorageService.save(photos,dataSource);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

}