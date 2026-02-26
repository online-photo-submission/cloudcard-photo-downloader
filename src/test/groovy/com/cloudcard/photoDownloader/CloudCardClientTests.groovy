package com.cloudcard.photoDownloader

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.test.util.ReflectionTestUtils

import static com.cloudcard.photoDownloader.CloudCardClient.*
import static org.assertj.core.api.Assertions.assertThat
import static org.junit.Assume.assumeTrue
import static org.mockito.Mockito.*

@Disabled
@ExtendWith(MockitoExtension.class)
class CloudCardClientTests {

    private String[] fetchStatuses = [APPROVED, READY_FOR_DOWNLOAD];

    @Mock
    PreProcessor mockPreProcessor

    @Mock
    RestService mockRestService

    @Mock
    TokenService mockTokenService

    @InjectMocks
    CloudCardClient client

    @BeforeEach
    public void setup() {

        client = new CloudCardClient()
        ReflectionTestUtils.setField(client, "apiUrl", "http://localhost:8082/api")
        ReflectionTestUtils.setField(client, "fetchStatuses", fetchStatuses)
        ReflectionTestUtils.setField(client, "putStatus", DOWNLOADED)
        ReflectionTestUtils.setField(client, "restService", mockRestService)
        ReflectionTestUtils.setField(client, "preProcessor", mockPreProcessor)
        ReflectionTestUtils.setField(client, "tokenService", mockTokenService)
    }

    @Test
    public void testFetchReadyForDownload() throws Exception {
        Photo processedPhoto = new Photo()
        when(mockPreProcessor.process(any(Photo.class))).thenReturn(processedPhoto)
        when(mockTokenService.getAuthToken()).thenReturn("via76odv674i54eenhqvuf3v8cpverv79tj1bhak9u4u0ktheqa3qg2186srrt1g")

        List<Photo> photos = client.fetchWithBytes(READY_FOR_DOWNLOAD)()
        assertThat(photos.size()).isGreaterThanOrEqualTo(0)
        for (Photo photo : photos) {

            assertThat(fetchStatuses).contains(photo.getStatus())

            verify(mockPreProcessor, times(1)).process(photo)
        }

        verify(mockRestService, times(photos.size())).fetchBytes(processedPhoto)
        verify(mockTokenService, times(4)).getAuthToken()
    }

    @Test
    public void testMarkAsDownloaded() throws Exception {

        assumeTrue(!client.fetchWithBytes(READY_FOR_DOWNLOAD).isEmpty())

        Photo photo = client.fetchWithBytes(READY_FOR_DOWNLOAD).get(0)
        int originalReadyCount = client.fetchWithBytes(READY_FOR_DOWNLOAD).size()
        int originalDownloadedCount = client.fetch(DOWNLOADED).size()

        Photo updatedPhoto = client.updateStatus(photo, DOWNLOADED)

        List<Photo> downloadedPhotos = client.fetch(DOWNLOADED)
        assertThat(updatedPhoto).isNotNull()
        assertThat(updatedPhoto.getStatus()).isEqualTo(DOWNLOADED)

        assertThat(client.fetchWithBytes(READY_FOR_DOWNLOAD).size()).isEqualTo(originalReadyCount - 1)
        assertThat(downloadedPhotos.size()).isEqualTo(originalDownloadedCount + 1)

        client.updateStatus(photo, DOWNLOADED)

        assertThat(client.fetchWithBytes(READY_FOR_DOWNLOAD).size()).isEqualTo(originalReadyCount)
        assertThat(client.fetch(DOWNLOADED).size()).isEqualTo(originalDownloadedCount)
    }
}
