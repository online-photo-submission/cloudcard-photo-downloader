package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static com.cloudcard.photoDownloader.CloudCardPhotoService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class CloudCardPhotoServiceTests {

    private String[] fetchStatuses = {"APPROVED", "READY_FOR_DOWNLOAD"};

    @Mock
    PreProcessor mockPreProcessor;

    @Mock
    RestService mockRestService;

    @InjectMocks
    CloudCardPhotoService service;

    @Before
    public void setup() {

        service = new CloudCardPhotoService();
        ReflectionTestUtils.setField(service, "apiUrl", "http://localhost:8080/api");
        ReflectionTestUtils.setField(service, "accessToken", "tu34pec33k4uait0u12f6c4jujll9s3d");
        ReflectionTestUtils.setField(service, "fetchStatuses", fetchStatuses);
        ReflectionTestUtils.setField(service, "putStatus", "DOWNLOADED");
        ReflectionTestUtils.setField(service, "restService", mockRestService);
        ReflectionTestUtils.setField(service, "preProcessor", mockPreProcessor);
    }

    @Test
    public void testFetchReadyForDownload() throws Exception {
        Photo processedPhoto = new Photo();
        when(mockPreProcessor.process(any(Photo.class))).thenReturn(processedPhoto);

        List<Photo> photos = service.fetchReadyForDownload();
        assertThat(photos.size()).isGreaterThanOrEqualTo(0);
        for (Photo photo : photos) {

            assertThat(fetchStatuses).contains(photo.getStatus());

            verify(mockPreProcessor, times(1)).process(photo);
        }

        verify(mockRestService, times(photos.size())).fetchBytes(processedPhoto);
    }

    @Test
    public void testMarkAsDownloaded() throws Exception {

        assumeTrue(!service.fetchReadyForDownload().isEmpty());

        Photo photo = service.fetchReadyForDownload().get(0);
        int originalReadyCount = service.fetchReadyForDownload().size();
        int originalDownloadedCount = service.fetch(DOWNLOADED).size();

        Photo updatedPhoto = service.markAsDownloaded(photo);

        List<Photo> downloadedPhotos = service.fetch(DOWNLOADED);
        assertThat(updatedPhoto).isNotNull();
        assertThat(updatedPhoto.getStatus()).isEqualTo(DOWNLOADED);

        assertThat(service.fetchReadyForDownload().size()).isEqualTo(originalReadyCount - 1);
        assertThat(downloadedPhotos.size()).isEqualTo(originalDownloadedCount + 1);

        service.updateStatus(photo, READY_FOR_DOWNLOAD);

        assertThat(service.fetchReadyForDownload().size()).isEqualTo(originalReadyCount);
        assertThat(service.fetch(DOWNLOADED).size()).isEqualTo(originalDownloadedCount);
    }
}
