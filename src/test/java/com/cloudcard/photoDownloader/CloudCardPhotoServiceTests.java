package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.cloudcard.photoDownloader.CloudCardPhotoService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assume.assumeTrue;

//@Ignore
public class CloudCardPhotoServiceTests {

    private static final String API_URL = "https://api.onlinephotosubmission.com/api";
    private static final String ACCESS_TOKEN = "bom0m9qvfpkr42rv3odo41feilmvj740";

    CloudCardPhotoService service;

    @Before
    public void setup() {

        service = new CloudCardPhotoService(API_URL, ACCESS_TOKEN);
    }

    @Test
    public void testFetchReadyForDownload() throws Exception {

        List<Photo> photos = service.fetchReadyForDownload();
        assertThat(photos.size()).isGreaterThanOrEqualTo(0);
        for (Photo photo : photos) {
            assertThat(photo.getStatus()).isEqualTo(READY_FOR_DOWNLOAD);
        }
    }

    @Test
    public void testFetchApproved() throws Exception {


        List<Photo> photos = service.fetchApproved();
        assertThat(photos.size()).isGreaterThanOrEqualTo(0);
        for (Photo photo : photos) {
            assertThat(photo.getStatus()).isEqualTo(APPROVED);
        }
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
