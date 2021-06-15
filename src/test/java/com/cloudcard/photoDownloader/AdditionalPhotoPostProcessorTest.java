package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Random;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AdditionalPhotoPostProcessorTest {

    @Mock
    FileService mockFileService;

    @Mock
    RestService mockRestService;

    @InjectMocks
    AdditionalPhotoPostProcessor postProcessor;
    Random random = new Random();

    @Before
    public void setUp() {

    }

    @Test
    public void testProcess_WithOneAdditionalPhoto() throws Exception {

        //set up
        AdditionalPhoto additionalPhoto = createAdditionalPhoto("pancakes");
        Photo photo = createPhoto(additionalPhoto);
        String baseFileName = "id" + random.nextInt();

        //run the test
        postProcessor.process(photo, "./temp", new PhotoFile(baseFileName, null, 1));

        //check stuff
        verify(mockRestService, times(1)).fetchBytes(additionalPhoto);
        verify(mockFileService, times(1)).writeBytesToFile("./temp/pancakes", baseFileName + ".jpg", additionalPhoto.getBytes());
    }

    @Test
    public void testProcess_WithTwoAdditionalPhotos() throws Exception {

        //set up
        AdditionalPhoto additionalPhoto = createAdditionalPhoto("pancakes");
        AdditionalPhoto additionalPhoto2 = createAdditionalPhoto("sausage");
        Photo photo = createPhoto(additionalPhoto);
        photo.getPerson().getAdditionalPhotos().add(additionalPhoto2);
        String baseFileName = "id" + random.nextInt();

        //run the test
        postProcessor.process(photo, "./temp", new PhotoFile(baseFileName, null, 1));

        //check stuff
        verify(mockRestService, times(1)).fetchBytes(additionalPhoto);
        verify(mockFileService, times(1)).writeBytesToFile("./temp/pancakes", baseFileName + ".jpg", additionalPhoto.getBytes());

        verify(mockRestService, times(1)).fetchBytes(additionalPhoto2);
        verify(mockFileService, times(1)).writeBytesToFile("./temp/sausage", baseFileName + ".jpg", additionalPhoto2.getBytes());
    }

    @Test
    public void testInclude() throws Exception {

        //set up
        AdditionalPhoto additionalPhoto = createAdditionalPhoto("pancakes");
        AdditionalPhoto additionalPhoto2 = createAdditionalPhoto("sausage");
        Photo photo = createPhoto(additionalPhoto);
        photo.getPerson().getAdditionalPhotos().add(additionalPhoto2);
        String baseFileName = "id" + random.nextInt();

        //and configure the post-processor to only process one of the additional photo types
        String[] include = {"pancakes"};
        ReflectionTestUtils.setField(postProcessor, "include", include);

        //run the test
        postProcessor.process(photo, "./temp", new PhotoFile(baseFileName, null, 1));

        //check stuff
        verify(mockRestService, times(1)).fetchBytes(additionalPhoto);
        verify(mockFileService, times(1)).writeBytesToFile("./temp/pancakes", baseFileName + ".jpg", additionalPhoto.getBytes());

        verify(mockRestService, times(0)).fetchBytes(additionalPhoto2);
        verify(mockFileService, times(0)).writeBytesToFile("./temp/sausage", baseFileName + ".jpg", additionalPhoto2.getBytes());
    }

    /* *** PRIVATE HELPER METHODS *** */

    private AdditionalPhoto createAdditionalPhoto(String typeName) {

        AdditionalPhoto additionalPhoto = new AdditionalPhoto();
        additionalPhoto.setAdditionalPhotoType(new AdditionalPhotoType());
        ReflectionTestUtils.setField(additionalPhoto, "typeName", typeName);
        byte[] expectedBytes = {0, 1, 2, 3, 4};
        additionalPhoto.setBytes(expectedBytes);
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