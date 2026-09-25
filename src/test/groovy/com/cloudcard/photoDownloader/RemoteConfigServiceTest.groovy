package com.cloudcard.photoDownloader

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.springframework.test.util.ReflectionTestUtils

import java.time.Instant

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatThrownBy
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.*

class RemoteConfigServiceTest {

    private static final String API_URL = "http://localhost:8082/api"
    private static final String INTEGRATION_NAME = "downloader"
    private static final String PAT = "pat-value"
    private static final String MANAGED_TOKEN = "managed-token"
    private static final String MANUAL_TOKEN = "manual-token"

    RemoteConfigService service
    MockedStatic<RemotePhotoUtil> remotePhotoUtil

    @BeforeEach
    void setup() {
        service = new RemoteConfigService()
        remotePhotoUtil = mockStatic(RemotePhotoUtil)
    }

    @AfterEach
    void teardown() {
        remotePhotoUtil.close()
    }

    /* *** FETCHING CONFIG *** */

    @Test
    void fetchRemoteConfigUsesTheManagedTokenService() {
        useManagedToken()

        Integration expected = new Integration(version: 7)

        remotePhotoUtil.when {
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN)
        }.thenReturn(expected)

        Integration result = service.fetchRemoteConfig()

        assertThat(result).isSameAs(expected)

        remotePhotoUtil.verify({
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN)
        }, times(1))

        remotePhotoUtil.verify({
            RemotePhotoUtil.login(anyString(), anyString())
        }, never())

        remotePhotoUtil.verify({
            RemotePhotoUtil.logout(anyString(), anyString())
        }, never())
    }

    @Test
    void fetchRemoteConfigUsesManualLoginWhenThereIsNoTokenService() {
        service.apiUrl = API_URL
        service.integrationName = INTEGRATION_NAME
        service.pat = PAT
        service.tokenService = null

        AuthenticationToken authenticationToken =
            new AuthenticationToken(tokenValue: MANUAL_TOKEN)

        Integration expected = new Integration(version: 7)

        remotePhotoUtil.when {
            RemotePhotoUtil.login(API_URL, PAT)
        }.thenReturn(authenticationToken)

        remotePhotoUtil.when {
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANUAL_TOKEN)
        }.thenReturn(expected)

        Integration result = service.fetchRemoteConfig()

        assertThat(result).isSameAs(expected)

        remotePhotoUtil.verify({
            RemotePhotoUtil.login(API_URL, PAT)
        }, times(1))

        remotePhotoUtil.verify({
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANUAL_TOKEN)
        }, times(1))

        remotePhotoUtil.verify({
            RemotePhotoUtil.logout(API_URL, MANUAL_TOKEN)
        }, times(1))
    }

    @Test
    void manualFetchLogsOutEvenWhenConfigFetchFails() {
        service.apiUrl = API_URL
        service.integrationName = INTEGRATION_NAME
        service.pat = PAT
        service.tokenService = null

        AuthenticationToken authenticationToken =
            new AuthenticationToken(tokenValue: MANUAL_TOKEN)

        def failure = new RuntimeException("Config API down")

        remotePhotoUtil.when {
            RemotePhotoUtil.login(API_URL, PAT)
        }.thenReturn(authenticationToken)

        remotePhotoUtil.when {
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANUAL_TOKEN)
        }.thenThrow(failure)

        assertThatThrownBy {
            service.fetchRemoteConfig()
        }.isSameAs(failure)

        remotePhotoUtil.verify({
            RemotePhotoUtil.logout(API_URL, MANUAL_TOKEN)
        }, times(1))
    }

    /* *** REFRESH DECISIONS *** */

    @Test
    void shouldRefreshReturnsTrueAndUpdatesCurrentVersionWhenVersionChanges() {
        useManagedToken()
        service.currentVersion = 6

        remotePhotoUtil.when {
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN)
        }.thenReturn(new Integration(version: 7))

        assertThat(service.shouldRefresh()).isTrue()
        assertThat(service.currentVersion).isEqualTo(7)
    }

    @Test
    void shouldRefreshReturnsFalseWhenVersionHasNotChanged() {
        useManagedToken()
        service.currentVersion = 7

        remotePhotoUtil.when {
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN)
        }.thenReturn(new Integration(version: 7))

        assertThat(service.shouldRefresh()).isFalse()
        assertThat(service.currentVersion).isEqualTo(7)
    }

    @Test
    void shouldRefreshThrottlesSubsequentChecks() {
        useManagedToken()
        service.currentVersion = 6

        remotePhotoUtil.when {
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN)
        }.thenReturn(new Integration(version: 7))

        assertThat(service.shouldRefresh()).isTrue()
        assertThat(service.shouldRefresh()).isFalse()

        remotePhotoUtil.verify({
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN)
        }, times(1))
    }

    @Test
    void shouldRefreshThrottlesFailedChecksToo() {
        useManagedToken()

        def failure = new RuntimeException("Config API down")

        remotePhotoUtil.when {
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN)
        }.thenThrow(failure)

        assertThat(service.shouldRefresh()).isFalse()
        assertThat(service.shouldRefresh()).isFalse()

        remotePhotoUtil.verify({
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN)
        }, times(1))
    }

    @Test
    void shouldRefreshChecksAgainAfterTheInterval() {
        useManagedToken()
        service.currentVersion = 7

        remotePhotoUtil.when {
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN)
        }.thenReturn(new Integration(version: 7))

        assertThat(service.shouldRefresh()).isFalse()

        ReflectionTestUtils.setField(service, "nextCheck", Instant.MIN)

        assertThat(service.shouldRefresh()).isFalse()

        remotePhotoUtil.verify({
            RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN)
        }, times(2))
    }

    /* *** HELPERS *** */

    private void useManagedToken() {
        service.apiUrl = API_URL
        service.integrationName = INTEGRATION_NAME

        TokenService tokenService = mock(TokenService)
        when(tokenService.getAuthTokenValue()).thenReturn(MANAGED_TOKEN)

        service.tokenService = tokenService
    }
}