package com.cloudcard.photoDownloader

import com.fasterxml.jackson.annotation.JsonAnyGetter
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
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

    @Autowired
    RestService restService

    @PostConstruct
    void init() {
        if (this.persistentAccessTokenIsEmpty()) {
            log.info("Persist. Access Token not set.")
        } else {
            log.info("Persist. Access Token : ..." + persistentAccessToken.substring(3, 8) + "...")
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

//    TODO: Is this token good until it expires? When does it expire?
//      the token is logged out by the CloudCard Client when it closes, which with SQS will happen every 10 photos?
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

    @JsonAnyGetter
    String getAuthTokenValue() {
        if (authenticationTokenIsEmpty() || isExpiring()) {
            try {
                this.login()
            } catch (Exception e) {
                log.error("Error while trying to retrieve token from CloudCard API.", e)
                return null
            }
        }

        return authenticationToken.tokenValue
    }

    //    TODO: TEST EXPIRY REFRESH
    private boolean isExpiring() {
        authenticationToken.expirationDate
            .toInstant()
            .isBefore(Instant.now() + REFRESH_BUFFER_MINUTES)
    }

}
