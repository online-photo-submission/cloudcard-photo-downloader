package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

public class FileStoreServiceTests {

    FileStorageService fileStorageService;
    private Person person;
    private Photo photo;

    @Before
    public void setup() {

        fileStorageService = new FileStorageService("", 8);

        person = new Person();
        photo = new Photo();
        photo.setPerson(person);
    }

    @Test
    public void getStudentID_should_pad_id_correctly() {

        String identifier = "123";

        testGetStudentId(identifier, 8, "00000123");
        testGetStudentId(identifier, 10, "0000000123");
        testGetStudentId(identifier, 0, identifier);
        testGetStudentId(identifier, 2, identifier);
    }

    @Test
    public void getStudentID_should_not_throw_NPE() {

        testGetStudentId(null, 8, null);
    }

    @Test
    public void getStudentID_should_not_pad_an_empty_id() {

        testGetStudentId("", 8, "");
    }

    private void testGetStudentId(String identifier, int minPhotoIdLength, String expected) {

        person.setIdentifier(identifier);
        ReflectionTestUtils.setField(fileStorageService, "minPhotoIdLength", minPhotoIdLength);
        String result = ReflectionTestUtils.invokeMethod(fileStorageService, "getStudentID", photo);
        assertThat(result).isEqualTo(expected);
    }
}
