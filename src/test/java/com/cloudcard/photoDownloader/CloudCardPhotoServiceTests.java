package com.cloudcard.photoDownloader;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

//@Ignore
public class CloudCardPhotoServiceTests {

    private static final String API_URL = "https://api.onlinephotosubmission.com/api";
    private static final String ACCESS_TOKEN = "bom0m9qvfpkr42rv3odo41feilmvj740";

    CloudCardPhotoService service;

    @Before
    public void setup() {

        service = new CloudCardPhotoService(API_URL, ACCESS_TOKEN);
    }

    //
    @Test
    public void testFetchReadyForDownload() throws Exception {

        assertThat(service.fetchReadyForDownload().size()).isEqualTo(5);
    }

    @Test
    public void testFetchApproved() throws Exception {

        assertThat(service.fetchApproved().size()).isEqualTo(1);
    }
}
