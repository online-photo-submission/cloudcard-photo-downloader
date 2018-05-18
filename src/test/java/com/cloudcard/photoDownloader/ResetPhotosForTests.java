package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;

import static com.cloudcard.photoDownloader.CloudCardPhotoService.APPROVED;
import static com.cloudcard.photoDownloader.CloudCardPhotoService.READY_FOR_DOWNLOAD;

//@Ignore
public class ResetPhotosForTests {

    private static final String API_URL = "https://api.onlinephotosubmission.com/api";
    private static final String ACCESS_TOKEN = "bom0m9qvfpkr42rv3odo41feilmvj740";

    CloudCardPhotoService service;

    @Before
    public void setup() {

        service = new CloudCardPhotoService(API_URL, ACCESS_TOKEN);
    }

    @Test
    public void resetPhotos() throws Exception {

        service.updateStatus(new Photo(107307), READY_FOR_DOWNLOAD);
        service.updateStatus(new Photo(107311), READY_FOR_DOWNLOAD);
        service.updateStatus(new Photo(107312), READY_FOR_DOWNLOAD);
        service.updateStatus(new Photo(107313), READY_FOR_DOWNLOAD);
        service.updateStatus(new Photo(112721), READY_FOR_DOWNLOAD);
        service.updateStatus(new Photo(116194), APPROVED);
    }
}
