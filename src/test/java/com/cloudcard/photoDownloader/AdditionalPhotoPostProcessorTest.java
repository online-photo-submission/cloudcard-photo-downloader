package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AdditionalPhotoPostProcessorTest {

    @Mock
    FileService mockFileService;

    @InjectMocks
    AdditionalPhotoPostProcessor postProcessor;
    Random random = new Random();

    @Before
    public void setUp() {

    }

    @Test
    public void testProcess_WithOneAdditionalPhotot() throws Exception {

        //set up
        AdditionalPhoto additionalPhoto = createAdditionalPhoto("https://sharptopco.github.io/cloudcard-custom-assets/example_id_photo.jpg", "pancakes");
        Photo photo = createPhoto(additionalPhoto);
        String baseFileName = "id" + random.nextInt();

        //run the test
        postProcessor.process(photo, "./temp", new PhotoFile(baseFileName, null, 1));

        //check stuff
        byte[] expectedBytes = Files.readAllBytes(Paths.get("src/test/resources/example_id_photo.jpg"));
        verify(mockFileService, times(1)).writeBytesToFile("./temp/pancakes", baseFileName + ".jpg", expectedBytes);
    }

    @Test
    public void testProcess_WithTwoAdditionalPhotos() throws Exception {

        //set up
        AdditionalPhoto additionalPhoto = createAdditionalPhoto("https://sharptopco.github.io/cloudcard-custom-assets/example_id_photo.jpg", "pancakes");
        AdditionalPhoto additionalPhoto2 = createAdditionalPhoto("https://sharptopco.github.io/cloudcard-custom-assets/not-submitted.jpg", "sausage");
        Photo photo = createPhoto(additionalPhoto);
        photo.getPerson().getAdditionalPhotos().add(additionalPhoto2);
        String baseFileName = "id" + random.nextInt();

        //run the test
        postProcessor.process(photo, "./temp", new PhotoFile(baseFileName, null, 1));

        //check stuff
        byte[] expectedBytes = Files.readAllBytes(Paths.get("src/test/resources/example_id_photo.jpg"));
        verify(mockFileService, times(1)).writeBytesToFile("./temp/pancakes", baseFileName + ".jpg", expectedBytes);

        expectedBytes = Files.readAllBytes(Paths.get("src/test/resources/not-submitted.jpg"));
        verify(mockFileService, times(1)).writeBytesToFile("./temp/sausage", baseFileName + ".jpg", expectedBytes);
    }

    /* *** PRIVATE HELPER METHODS *** */

    private AdditionalPhoto createAdditionalPhoto(String externalURL, String typeName) {

        AdditionalPhoto additionalPhoto = new AdditionalPhoto();
        additionalPhoto.setExternalURL(externalURL);
        additionalPhoto.setAdditionalPhotoType(new AdditionalPhotoType());
        ReflectionTestUtils.setField(additionalPhoto, "typeName", typeName);
        return additionalPhoto;
    }

    private Photo createPhoto(AdditionalPhoto additionalPhoto) {

        Photo photo = new Photo();
        photo.setPerson(new Person());
        photo.getPerson().setAdditionalPhotos(new ArrayList<>());
        photo.getPerson().getAdditionalPhotos().add(additionalPhoto);
        return photo;
    }

}