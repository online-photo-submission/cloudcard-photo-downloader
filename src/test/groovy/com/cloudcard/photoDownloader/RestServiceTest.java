package com.cloudcard.photoDownloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class RestServiceTest {

    RestService restService;
    final static String TEST_IMAGE_URL = "https://sharptopco.github.io/cloudcard-custom-assets/example_id_photo.jpg";
    static byte[] expectedTestImageBytes;

    @BeforeEach
    public void setUp() throws IOException {
        restService = new RestService();
        expectedTestImageBytes = Files.readAllBytes(Paths.get("src/test/resources/example_id_photo.jpg"));
    }

    @Test
    public void testFetchBytesForPhoto() throws Exception {

        //setup
        Photo photo = new Photo();
        photo.setExternalURL(TEST_IMAGE_URL);

        //test
        restService.fetchBytes(photo);

        //verify
        assertThat(photo.getBytes()).isEqualTo(expectedTestImageBytes);
    }

    @Test
    public void testFetchBytesForAdditionalPhoto() throws Exception {

        //setup
        AdditionalPhoto additionalPhoto = new AdditionalPhoto();
        additionalPhoto.setExternalURL(TEST_IMAGE_URL);

        //test
        restService.fetchBytes(additionalPhoto);

        //verify
        assertThat(additionalPhoto.getBytes()).isEqualTo(expectedTestImageBytes);
    }
}
