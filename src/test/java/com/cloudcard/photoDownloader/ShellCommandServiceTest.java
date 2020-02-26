package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ShellCommandServiceTest {

    public static final String DUMMY_COMMAND = "/Users/terskine/git/online-photo-submission/cloudcard-photo-downloader/dummy-script.sh";

    @Mock
    ShellCommandRunner mockShellCommandRunner;

    @InjectMocks
    ShellCommandService service;

    @Before
    public void before() {

        service.preExecuteCommand = "bacon";
        service.preDownloadCommand = "eggs";
        service.postDownloadCommand = "sausage";
        service.postExecuteCommand = "biscuits";

        when(mockShellCommandRunner.run(eq(service.preExecuteCommand))).thenReturn(true);
        when(mockShellCommandRunner.run(eq(service.preDownloadCommand))).thenReturn(true);
        when(mockShellCommandRunner.run(eq(service.postDownloadCommand))).thenReturn(true);
        when(mockShellCommandRunner.run(eq(service.postExecuteCommand))).thenReturn(true);

    }

    @Test
    public void testPreExecute() {

        assertThat(service.preExecute()).isTrue();

        verify(mockShellCommandRunner, times(1)).run(service.preExecuteCommand);
    }

    @Test
    public void testPreDownloadWithPhotoFiles() {

        assertThat(service.preDownload(generatePhotoList())).isTrue();

        verify(mockShellCommandRunner, times(1)).run(service.preDownloadCommand);
    }

    @Test
    public void testPreDownloadWithoutPhotoFiles() {

        List<Photo> emptyList = new ArrayList<>();
        assertThat(service.preDownload(emptyList)).isTrue();
        assertThat(service.preDownload(null)).isTrue();

        verifyZeroInteractions(mockShellCommandRunner);
    }

    @Test
    public void testPostDownloadWithPhotoFiles() {

        assertThat(service.postDownload(generatePhotoFileList())).isTrue();

        verify(mockShellCommandRunner, times(1)).run(service.postDownloadCommand);
    }

    @Test
    public void testPostDownloadWithoutPhotoFiles() {

        List<PhotoFile> emptyList = new ArrayList<>();
        assertThat(service.postDownload(emptyList)).isTrue();
        assertThat(service.postDownload(null)).isTrue();

        verifyZeroInteractions(mockShellCommandRunner);
    }

    @Test
    public void testPostExecute() {

        assertThat(service.postExecute()).isTrue();

        verify(mockShellCommandRunner, times(1)).run(service.postExecuteCommand);
    }

    @Test
    public void testEmptyCommands() {

        //setup
        service.preExecuteCommand = "";
        service.preDownloadCommand = "";
        service.postDownloadCommand = "";
        service.postExecuteCommand = "";

        //test
        service.preExecute();
        service.postExecute();
        service.preDownload(generatePhotoList());
        service.postDownload(generatePhotoFileList());

        //verify
        verifyZeroInteractions(mockShellCommandRunner);
    }

    /* *** PRIVATE HELPERS *** */

    private List<PhotoFile> generatePhotoFileList() {

        List<PhotoFile> photoFiles = new ArrayList<>();
        photoFiles.add(new PhotoFile("", "", 0));
        return photoFiles;
    }

    private List<Photo> generatePhotoList() {

        List<Photo> photos = new ArrayList<>();
        photos.add(new Photo());
        return photos;
    }

}