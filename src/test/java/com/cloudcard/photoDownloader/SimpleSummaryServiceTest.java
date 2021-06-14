package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static java.time.LocalDateTime.now;
import static java.time.format.DateTimeFormatter.ofPattern;
import static org.mockito.Mockito.*;

//import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class SimpleSummaryServiceTest {

    @Mock
    FileService mockFileService;

    @InjectMocks
    SimpleSummaryService service;

    @Before
    public void before() throws IOException {

        ReflectionTestUtils.setField(service, "fileName", "");
        ReflectionTestUtils.setField(service, "directory", "summary");
    }

    @Test
    public void testWriteFile_Zero() throws Exception {
        //setup
        List<String> expectedLines = new ArrayList<>();
        expectedLines.add(now().format(ofPattern("MMM-dd HH:mm")) + " | Attempted: 1       | Succeeded: 1       | Failed: 0       ");

        service.createSummary(generatePhotoList(0), generatePhotoFileList(0));

        verifyZeroInteractions(mockFileService);
    }

    @Test
    public void testWriteFile_One() throws Exception {
        //setup
        List<String> expectedLines = new ArrayList<>();
        expectedLines.add(now().format(ofPattern("MMM-dd HH:mm")) + " | Attempted: 1       | Succeeded: 1       | Failed: 0       ");

        service.createSummary(generatePhotoList(), generatePhotoFileList());

        verify(mockFileService, times(1)).writeFile(expectedLines, "summary/cloudcard-download-summary_" + LocalDate.now() + ".txt");
    }

    @Test
    public void testWriteFile_Multiple() throws Exception {
        //setup
        ReflectionTestUtils.setField(service, "directory", "eggs");
        ReflectionTestUtils.setField(service, "fileName", "bacon");
        List<String> expectedLines = new ArrayList<>();
        expectedLines.add(now().format(ofPattern("MMM-dd HH:mm")) + " | Attempted: 12345   | Succeeded: 1234    | Failed: 11111   ");

        service.createSummary(generatePhotoList(12345), generatePhotoFileList(1234));

        verify(mockFileService, times(1)).writeFile(expectedLines, "eggs/bacon");
    }

    /* *** PRIVATE HELPERS *** */

    private List<PhotoFile> generatePhotoFileList() {

        return generatePhotoFileList(1);
    }

    private List<PhotoFile> generatePhotoFileList(int count) {

        List<PhotoFile> photoFiles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            photoFiles.add(new PhotoFile("baseName" + i, "fileName" + i, i));
        }
        return photoFiles;
    }

    private List<Photo> generatePhotoList() {

        return generatePhotoList(1);
    }

    private List<Photo> generatePhotoList(int count) {

        List<Photo> photos = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            photos.add(new Photo(i));
        }
        return photos;
    }
}