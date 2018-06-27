package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class FileStoreServiceTests {

    FileStorageService fileStorageService;

    @Before
    public void setup() {

        fileStorageService = new FileStorageService("", 8);
    }

    @Test
    public void testGetIdentifier() {

        Person person = new Person();
        person.setIdentifier("123");
        Photo photo = new Photo();
        photo.setPerson(person);

        ReflectionTestUtils.setField(fileStorageService, "minPhotoIdLength", 8);
        String result = ReflectionTestUtils.invokeMethod(fileStorageService, "getStudentID", photo);
        assertThat(result).isEqualTo("00000123");

        ReflectionTestUtils.setField(fileStorageService, "minPhotoIdLength", 10);
        result = ReflectionTestUtils.invokeMethod(fileStorageService, "getStudentID", photo);
        assertThat(result).isEqualTo("0000000123");

        ReflectionTestUtils.setField(fileStorageService, "minPhotoIdLength", 0);
        result = ReflectionTestUtils.invokeMethod(fileStorageService, "getStudentID", photo);
        assertThat(result).isEqualTo("123");

        ReflectionTestUtils.setField(fileStorageService, "minPhotoIdLength", 2);
        result = ReflectionTestUtils.invokeMethod(fileStorageService, "getStudentID", photo);
        assertThat(result).isEqualTo("123");
    }
}
