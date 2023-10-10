package com.cloudcard.photoDownloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static com.cloudcard.photoDownloader.CloudCardPhotoService.DOWNLOADED;
import static com.cloudcard.photoDownloader.CloudCardPhotoService.READY_FOR_DOWNLOAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.mockito.Mockito.*;

@Disabled
@ExtendWith(MockitoExtension.class)
public class CloudCardPhotoServiceTests {

    private String[] fetchStatuses = {"APPROVED", "READY_FOR_DOWNLOAD"};

    @Mock
    PreProcessor mockPreProcessor;

    @Mock
    RestService mockRestService;

    @Mock
    TokenService mockTokenService;

    @InjectMocks
    CloudCardPhotoService service;

    @BeforeEach
    public void setup() {

        service = new CloudCardPhotoService();
        ReflectionTestUtils.setField(service, "apiUrl", "http://localhost:8082/api");
        ReflectionTestUtils.setField(service, "fetchStatuses", fetchStatuses);
        ReflectionTestUtils.setField(service, "putStatus", "DOWNLOADED");
        ReflectionTestUtils.setField(service, "restService", mockRestService);
        ReflectionTestUtils.setField(service, "preProcessor", mockPreProcessor);
        ReflectionTestUtils.setField(service, "tokenService", mockTokenService);
    }

    @Test
    public void testFetchReadyForDownload() throws Exception {
        Photo processedPhoto = new Photo();
        when(mockPreProcessor.process(any(Photo.class))).thenReturn(processedPhoto);
        when(mockTokenService.getAuthToken()).thenReturn("via76odv674i54eenhqvuf3v8cpverv79tj1bhak9u4u0ktheqa3qg2186srrt1g");

        List<Photo> photos = service.fetchReadyForDownload();
        assertThat(photos.size()).isGreaterThanOrEqualTo(0);
        for (Photo photo : photos) {

            assertThat(fetchStatuses).contains(photo.getStatus());

            verify(mockPreProcessor, times(1)).process(photo);
        }

        verify(mockRestService, times(photos.size())).fetchBytes(processedPhoto);
        verify(mockTokenService, times(4)).getAuthToken();
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
