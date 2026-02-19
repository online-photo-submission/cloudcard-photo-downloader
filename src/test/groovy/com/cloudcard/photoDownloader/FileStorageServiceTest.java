package com.cloudcard.photoDownloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileStorageServiceTest {

    FileStorageService fileStorageService;

    @BeforeEach
    public void setUp() {
        fileStorageService = new FileStorageService();
        fileStorageService.useCardholderGroupSubdirectories = true;
    }

    private Photo createPhoto(String groupName) {
        CardholderGroup cg = new CardholderGroup();
        cg.setName(groupName);
        Person person = new Person();
        person.setCardholderGroup(cg);
        Photo photo = new Photo();
        photo.setPerson(person);
        return photo;
    }

    @Test
    public void testGetDirectory() {
        String photoDirectory = "dir";
        Photo photo = createPhoto("group1");
        assertEquals("dir/group1", fileStorageService.getDirectory(photo, photoDirectory));
    }

    @Test
    public void groupDirectoryShouldBeSanitized() {
        String photoDirectory = "dir";
        Photo photo = createPhoto("../\\group1");
        assertEquals("dir/___group1", fileStorageService.getDirectory(photo, photoDirectory));
    }

    @Test
    public void emptyGroupNameShouldUseDefaultFolder() {
        String photoDirectory = "dir";
        Photo photo = createPhoto("");
        assertEquals("dir", fileStorageService.getDirectory(photo, photoDirectory));
    }

    @Test
    public void blankGroupNameShouldUseDefaultFolder() {
        String photoDirectory = "dir";
        Photo photo = createPhoto("   ");
        assertEquals("dir", fileStorageService.getDirectory(photo, photoDirectory));
    }
}
