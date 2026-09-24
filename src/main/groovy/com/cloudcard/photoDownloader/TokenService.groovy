package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

import java.time.Duration
import java.time.Instant

@Service
class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class)
    private static final Duration REFRESH_BUFFER_MINUTES = Duration.ofMinutes(15)

    @Value('${cloudcard.api.url}')
    private String apiUrl

    AuthenticationToken authenticationToken

    @Value('${cloudcard.api.accessToken}')
    private String persistentAccessToken

    @PostConstruct
    void init() {
        if (this.persistentAccessTokenIsEmpty()) {
            log.info("       Persistent Access Token not set.")
        } else {
            log.info("       Persistent Access Token : ..." + persistentAccessToken.substring(3, 8) + "...")
        }
    }

    boolean persistentAccessTokenIsEmpty() {
        return persistentAccessToken == null || persistentAccessToken.isEmpty()
    }

    boolean authenticationTokenIsEmpty() {
        return authenticationToken == null || authenticationToken.tokenValue.isEmpty()
    }

    boolean isConfigured() {
        return !persistentAccessTokenIsEmpty()
    }

    void login() throws Exception {
        authenticationToken = RemotePhotoUtil.login(apiUrl, persistentAccessToken)
    }

    void logout() throws Exception {
        if (authenticationTokenIsEmpty()) {
            return
        }

        RemotePhotoUtil.logout(apiUrl, authenticationToken.tokenValue)

        authenticationToken = null
    }

    synchronized String getAuthTokenValue() {
        if (authenticationTokenIsEmpty() || isExpiring()) {
            log.info("Fetching new AuthenticationToken from CloudCard API.")
            this.login()
        }

        return authenticationToken.tokenValue
    }

    private boolean isExpiring() {
        authenticationToken.expirationDate
            .toInstant()
            .isBefore(Instant.now() + REFRESH_BUFFER_MINUTES)
    }

}
