package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class AdditionalPhotoPostProcessorTest {

    AdditionalPhotoPostProcessor postProcessor;
    Random random = new Random();

    //    private String[] photoDirectories = {"APPROVED", "READY_FOR_DOWNLOAD"};

    @Before
    public void setUp() {

        postProcessor = new AdditionalPhotoPostProcessor();
        ReflectionTestUtils.setField(postProcessor, "fileUtil", new FileUtil());
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
        File file = new File("./temp/pancakes/" + baseFileName + ".jpg");
        assertThat(file.exists()).isTrue();
        assertThat(file.length()).isEqualTo(new File("src/test/resources/example_id_photo.jpg").length());
    }

    @Test
    public void testProcess_WithTwoAdditionalPhotot() throws Exception {

        //set up
        AdditionalPhoto additionalPhoto = createAdditionalPhoto("https://sharptopco.github.io/cloudcard-custom-assets/example_id_photo.jpg", "pancakes");
        AdditionalPhoto additionalPhoto2 = createAdditionalPhoto("https://sharptopco.github.io/cloudcard-custom-assets/not-submitted.jpg", "sausage");
        Photo photo = createPhoto(additionalPhoto);
        photo.getPerson().getAdditionalPhotos().add(additionalPhoto2);
        String baseFileName = "id" + random.nextInt();

        //run the test
        postProcessor.process(photo, "./temp", new PhotoFile(baseFileName, null, 1));

        //check stuff
        File file = new File("./temp/pancakes/" + baseFileName + ".jpg");
        assertThat(file.exists()).isTrue();
        file = new File("./temp/sausage/" + baseFileName + ".jpg");
        assertThat(file.exists()).isTrue();
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