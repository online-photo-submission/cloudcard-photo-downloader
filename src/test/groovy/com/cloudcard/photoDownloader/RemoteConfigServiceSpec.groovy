package com.cloudcard.photoDownloader

import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

import java.time.Instant

/**
 * RemotePhotoUtil is a global Groovy mock, so nothing here touches the network. Global mocks only
 * intercept calls from dynamically compiled Groovy, which RemoteConfigService is.
 */
class RemoteConfigServiceSpec extends Specification {

    private static final String API_URL = "http://localhost:8082/api"
    private static final String INTEGRATION_NAME = "downloader"
    private static final String PAT = "pat-value"
    private static final String MANAGED_TOKEN = "managed-token"
    private static final String MANUAL_TOKEN = "manual-token"

    RemoteConfigService service

    def setup() {
        GroovyMock(RemotePhotoUtil, global: true)

        service = new RemoteConfigService(apiUrl: API_URL, integrationName: INTEGRATION_NAME)
    }

    /* *** FETCHING CONFIG *** */

    def "fetchRemoteConfig uses the managed token service"() {
        given:
        useManagedToken()
        Integration expected = new Integration(version: 7)

        when:
        Integration result = service.fetchRemoteConfig()

        then:
        1 * RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN) >> expected
        0 * RemotePhotoUtil.login(_, _)
        0 * RemotePhotoUtil.logout(_, _)
        result.is(expected)
    }

    def "fetchRemoteConfig logs in, fetches, and logs out, in that order, without a token service"() {
        given:
        useManualLogin()
        Integration expected = new Integration(version: 7)

        when:
        Integration result = service.fetchRemoteConfig()

        then:
        1 * RemotePhotoUtil.login(API_URL, PAT) >> new AuthenticationToken(tokenValue: MANUAL_TOKEN)

        then:
        1 * RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANUAL_TOKEN) >> expected

        then:
        1 * RemotePhotoUtil.logout(API_URL, MANUAL_TOKEN)
        result.is(expected)
    }

    def "a manual fetch logs out even when the config fetch fails"() {
        given:
        useManualLogin()
        RuntimeException failure = new RuntimeException("Config API down")

        when:
        service.fetchRemoteConfig()

        then:
        1 * RemotePhotoUtil.login(API_URL, PAT) >> new AuthenticationToken(tokenValue: MANUAL_TOKEN)
        1 * RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANUAL_TOKEN) >> { throw failure }
        1 * RemotePhotoUtil.logout(API_URL, MANUAL_TOKEN)

        and:
        RuntimeException thrownException = thrown()
        thrownException.is(failure)
    }

    /* *** REFRESH DECISIONS *** */

    def "with current version #currentVersion and remote version #remoteVersion, shouldRefresh is #refresh"() {
        given:
        useManagedToken()
        service.currentVersion = currentVersion

        when:
        boolean result = service.shouldRefresh()

        then:
        1 * RemotePhotoUtil.getRemoteConfig(API_URL, INTEGRATION_NAME, MANAGED_TOKEN) >> new Integration(version: remoteVersion)
        result == refresh
        service.currentVersion == remoteVersion

        where:
        currentVersion | remoteVersion || refresh
        6              | 7             || true    // changed
        7              | 7             || false   // unchanged
        null           | 7             || true    // no baseline: fetch again
    }

    def "shouldRefresh throttles subsequent checks"() {
        given:
        useManagedToken()
        service.currentVersion = 6

        when:
        boolean first = service.shouldRefresh()
        boolean second = service.shouldRefresh()

        then:
        1 * RemotePhotoUtil.getRemoteConfig(_, _, _) >> new Integration(version: 7)
        first
        !second
    }

    def "shouldRefresh throttles failed checks too"() {
        given:
        useManagedToken()

        when:
        boolean first = service.shouldRefresh()
        boolean second = service.shouldRefresh()

        then:
        1 * RemotePhotoUtil.getRemoteConfig(_, _, _) >> { throw new RuntimeException("Config API down") }
        !first
        !second
    }

    def "shouldRefresh checks again after the interval"() {
        given:
        useManagedToken()
        service.currentVersion = 7

        when:
        service.shouldRefresh()
        ReflectionTestUtils.setField(service, "nextCheck", Instant.MIN)
        service.shouldRefresh()

        then:
        2 * RemotePhotoUtil.getRemoteConfig(_, _, _) >> new Integration(version: 7)
    }

    /* *** HELPERS *** */

    private void useManagedToken() {
        service.tokenService = Stub(TokenService) {
            getAuthTokenValue() >> MANAGED_TOKEN
        }
    }

    private void useManualLogin() {
        service.pat = PAT
        service.tokenService = null
    }
}