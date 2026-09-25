package com.cloudcard.photoDownloader

import org.springframework.test.util.ReflectionTestUtils
import spock.lang.Specification

import java.time.Duration
import java.time.Instant

/**
 * Token lifecycle only. RemotePhotoUtil is a global Groovy mock, so nothing here touches the
 * network. Global mocks only intercept calls from dynamically compiled Groovy, which TokenService is.
 *
 * Expiry is controlled by the token's expirationDate relative to now rather than by a clock, since
 * TokenService calls Instant.now() directly. The margins are wide enough that timing jitter can't
 * move a token across the 15-minute refresh buffer.
 */
class TokenServiceSpec extends Specification {

    private static final String API_URL = "http://localhost:8082/api"
    private static final String PERSISTENT_ACCESS_TOKEN = "pat-0123456789abcdef"

    TokenService tokenService

    def setup() {
        GroovyMock(RemotePhotoUtil, global: true)

        tokenService = new TokenService()
        ReflectionTestUtils.setField(tokenService, "apiUrl", API_URL)
        ReflectionTestUtils.setField(tokenService, "persistentAccessToken", PERSISTENT_ACCESS_TOKEN)
    }

    /* *** CONFIGURATION *** */

    def "is configured when a persistent access token is set"() {
        expect:
        tokenService.isConfigured()
    }

    def "is not configured when the persistent access token is '#pat'"() {
        given:
        ReflectionTestUtils.setField(tokenService, "persistentAccessToken", pat)

        expect:
        !tokenService.isConfigured()

        where:
        pat << [null, ""]
    }

    /* *** LOGIN *** */

    def "login stores the returned token"() {
        given:
        AuthenticationToken token = token("fresh", Duration.ofHours(1))

        when:
        tokenService.login()

        then:
        1 * RemotePhotoUtil.login(API_URL, PERSISTENT_ACCESS_TOKEN) >> token
        tokenService.authenticationToken.is(token)
    }

    /* *** TOKEN REUSE AND REFRESH *** */

    def "logs in when there is no token"() {
        when:
        String value = tokenService.authTokenValue

        then:
        1 * RemotePhotoUtil.login(API_URL, PERSISTENT_ACCESS_TOKEN) >> token("first", Duration.ofHours(1))
        value == "first"
    }

    def "a token expiring in #expiresIn logs in #logins time(s) and resolves to '#expected'"() {
        given:
        tokenService.authenticationToken = token("current", expiresIn)

        when:
        String value = tokenService.authTokenValue

        then:
        logins * RemotePhotoUtil.login(API_URL, PERSISTENT_ACCESS_TOKEN) >> token("refreshed", Duration.ofHours(1))
        value == expected

        where:
        expiresIn              || expected    | logins
        Duration.ofHours(1)    || "current"   | 0    // well outside the 15-minute buffer
        Duration.ofMinutes(30) || "current"   | 0    // outside the buffer
        Duration.ofMinutes(5)  || "refreshed" | 1    // inside the buffer
        Duration.ofMinutes(-1) || "refreshed" | 1    // already expired
    }

    def "reuses a token that is not expiring across calls"() {
        given:
        tokenService.authenticationToken = token("reused", Duration.ofHours(1))

        when:
        String first = tokenService.authTokenValue
        String second = tokenService.authTokenValue

        then:
        0 * RemotePhotoUtil.login(_, _)
        first == "reused"
        second == "reused"
    }

    def "propagates a login failure"() {
        given:
        RuntimeException failure = new RuntimeException("API down")

        when:
        tokenService.authTokenValue

        then:
        1 * RemotePhotoUtil.login(_, _) >> { throw failure }

        and:
        RuntimeException thrownException = thrown()
        thrownException.is(failure)
    }

    /* *** LOGOUT *** */

    def "logout invalidates and clears the token"() {
        given:
        tokenService.authenticationToken = token("to-invalidate", Duration.ofHours(1))

        when:
        tokenService.logout()

        then:
        1 * RemotePhotoUtil.logout(API_URL, "to-invalidate")
        tokenService.authenticationToken == null
    }

    def "logout is a no-op without a token"() {
        when:
        tokenService.logout()

        then:
        0 * RemotePhotoUtil.logout(_, _)
    }

    def "the next call after logout logs in again"() {
        given:
        tokenService.authenticationToken = token("old", Duration.ofHours(1))

        when:
        tokenService.logout()
        String value = tokenService.authTokenValue

        then:
        1 * RemotePhotoUtil.logout(API_URL, "old")
        1 * RemotePhotoUtil.login(API_URL, PERSISTENT_ACCESS_TOKEN) >> token("new", Duration.ofHours(1))
        value == "new"
    }

    /* *** EMPTINESS *** */

    def "a token value of '#tokenValue' is empty: #empty"() {
        given:
        tokenService.authenticationToken = tokenValue == null ? null : token(tokenValue, Duration.ofHours(1))

        expect:
        tokenService.authenticationTokenIsEmpty() == empty

        where:
        tokenValue || empty
        null       || true
        ""         || true
        "value"    || false
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