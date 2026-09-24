package com.cloudcard.photoDownloader

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.MockedStatic
import org.springframework.test.util.ReflectionTestUtils

import java.time.Duration
import java.time.Instant

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatThrownBy
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.*
/**
 * Token lifecycle only. RemotePhotoUtil is mocked, so nothing here touches the network.
 *
 * Expiry is controlled by the token's expirationDate relative to now rather than by a clock, since
 * TokenService calls Instant.now() directly. The margins are wide enough that timing jitter can't
 * move a token across the 15-minute refresh buffer.
 */
class TokenServiceTest {

    private static final String API_URL = "http://localhost:8082/api"
    private static final String PERSISTENT_ACCESS_TOKEN = "pat-0123456789abcdef"

    TokenService tokenService
    MockedStatic<RemotePhotoUtil> remotePhotoUtil

    @BeforeEach
    void setup() {
        remotePhotoUtil = mockStatic(RemotePhotoUtil.class)

        tokenService = new TokenService()
        ReflectionTestUtils.setField(tokenService, "apiUrl", API_URL)
        ReflectionTestUtils.setField(tokenService, "persistentAccessToken", PERSISTENT_ACCESS_TOKEN)
    }

    @AfterEach
    void teardown() {
        remotePhotoUtil.close()
    }

    /* *** CONFIGURATION *** */

    @Test
    void isConfiguredWhenAPersistentAccessTokenIsSet() {
        assertThat(tokenService.isConfigured()).isTrue()
    }

    @Test
    void isNotConfiguredWhenThePersistentAccessTokenIsMissing() {
        ReflectionTestUtils.setField(tokenService, "persistentAccessToken", null)
        assertThat(tokenService.isConfigured()).isFalse()

        ReflectionTestUtils.setField(tokenService, "persistentAccessToken", "")
        assertThat(tokenService.isConfigured()).isFalse()
    }

    /* *** LOGIN *** */

    @Test
    void loginStoresTheReturnedToken() {
        AuthenticationToken token = token("fresh", Duration.ofHours(1))
        remotePhotoUtil.when { RemotePhotoUtil.login(API_URL, PERSISTENT_ACCESS_TOKEN) }.thenReturn(token)

        tokenService.login()

        assertThat(tokenService.authenticationToken).isSameAs(token)
    }

    /* *** TOKEN REUSE AND REFRESH *** */

    @Test
    void getAuthTokenValueLogsInWhenThereIsNoToken() {
        remotePhotoUtil.when { RemotePhotoUtil.login(anyString(), anyString()) }
            .thenReturn(token("first", Duration.ofHours(1)))

        assertThat(tokenService.getAuthTokenValue()).isEqualTo("first")

        remotePhotoUtil.verify({ RemotePhotoUtil.login(API_URL, PERSISTENT_ACCESS_TOKEN) }, times(1))
    }

    @Test
    void getAuthTokenValueReusesATokenThatIsNotExpiring() {
        tokenService.authenticationToken = token("reused", Duration.ofHours(1))

        assertThat(tokenService.getAuthTokenValue()).isEqualTo("reused")
        assertThat(tokenService.getAuthTokenValue()).isEqualTo("reused")

        remotePhotoUtil.verify({ RemotePhotoUtil.login(anyString(), anyString()) }, never())
    }

    @Test
    void getAuthTokenValueRefreshesATokenInsideTheRefreshBuffer() {
        // 5 minutes left is inside the 15-minute buffer.
        tokenService.authenticationToken = token("expiring", Duration.ofMinutes(5))
        remotePhotoUtil.when { RemotePhotoUtil.login(anyString(), anyString()) }
            .thenReturn(token("refreshed", Duration.ofHours(1)))

        assertThat(tokenService.getAuthTokenValue()).isEqualTo("refreshed")

        remotePhotoUtil.verify({ RemotePhotoUtil.login(anyString(), anyString()) }, times(1))
    }

    @Test
    void getAuthTokenValueRefreshesAnExpiredToken() {
        tokenService.authenticationToken = token("expired", Duration.ofMinutes(-1))
        remotePhotoUtil.when { RemotePhotoUtil.login(anyString(), anyString()) }
            .thenReturn(token("refreshed", Duration.ofHours(1)))

        assertThat(tokenService.getAuthTokenValue()).isEqualTo("refreshed")
    }

    @Test
    void getAuthTokenValueDoesNotRefreshATokenOutsideTheBuffer() {
        // 30 minutes left is comfortably outside the 15-minute buffer.
        tokenService.authenticationToken = token("fine", Duration.ofMinutes(30))

        assertThat(tokenService.getAuthTokenValue()).isEqualTo("fine")

        remotePhotoUtil.verify({ RemotePhotoUtil.login(anyString(), anyString()) }, never())
    }

    @Test
    void getAuthTokenValuePropagatesLoginFailure() {
        def failure = new RuntimeException("API down")

        remotePhotoUtil.when { RemotePhotoUtil.login(anyString(), anyString()) }
            .thenThrow(failure)

        assertThatThrownBy {
            tokenService.getAuthTokenValue()
        }.isSameAs(failure)
    }

    /* *** LOGOUT *** */

    @Test
    void logoutInvalidatesAndClearsTheToken() {
        tokenService.authenticationToken = token("to-invalidate", Duration.ofHours(1))

        tokenService.logout()

        remotePhotoUtil.verify({ RemotePhotoUtil.logout(API_URL, "to-invalidate") }, times(1))
        assertThat(tokenService.authenticationToken).isNull()
    }

    @Test
    void logoutIsANoOpWithoutAToken() {
        tokenService.logout()

        remotePhotoUtil.verify({ RemotePhotoUtil.logout(anyString(), anyString()) }, never())
    }

    @Test
    void theNextCallAfterLogoutLogsInAgain() {
        tokenService.authenticationToken = token("old", Duration.ofHours(1))
        remotePhotoUtil.when { RemotePhotoUtil.login(anyString(), anyString()) }
            .thenReturn(token("new", Duration.ofHours(1)))

        tokenService.logout()

        assertThat(tokenService.getAuthTokenValue()).isEqualTo("new")
    }

    /* *** EMPTINESS *** */

    @Test
    void authenticationTokenIsEmptyReflectsWhetherATokenIsHeld() {
        assertThat(tokenService.authenticationTokenIsEmpty()).isTrue()

        tokenService.authenticationToken = token("", Duration.ofHours(1))
        assertThat(tokenService.authenticationTokenIsEmpty()).isTrue()

        tokenService.authenticationToken = token("value", Duration.ofHours(1))
        assertThat(tokenService.authenticationTokenIsEmpty()).isFalse()
    }

    /* *** HELPERS *** */

    /**
     * AuthenticationToken stores expirationDate as the ISO-8601 string the API sends and parses it
     * in its getter, so the fake has to be a string too -- a Date gets toString()'d and won't parse.
     */
    private static AuthenticationToken token(String value, Duration expiresIn) {
        return new AuthenticationToken(
            tokenValue: value,
            expirationDate: Instant.now().plus(expiresIn).toString())
    }
}