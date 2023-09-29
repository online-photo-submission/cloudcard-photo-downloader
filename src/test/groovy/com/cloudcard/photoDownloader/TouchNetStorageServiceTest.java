package com.cloudcard.photoDownloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
public class TouchNetStorageServiceTest {

    TouchNetStorageService touchNetStorageService;

    @BeforeEach
    public void setUp() {

        //TODO mock the TouchNetClient (first test the service logic iwthout mocking it, then mock it.


        touchNetStorageService = new TouchNetStorageService();
    }

    @Test
    public void testFetchBytesForPhoto() throws Exception {

        //TODO make this just call the save method.
        assert touchNetStorageService.apiOnline();

        assert touchNetStorageService.login();

        assert touchNetStorageService.accountPhotoApprove();

        assert touchNetStorageService.logout();
    }

}
