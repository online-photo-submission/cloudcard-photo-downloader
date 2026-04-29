package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final Logger log = LoggerFactory.getLogger(TokenService.class);

    @Value("${cloudcard.api.url}")
    private String apiUrl;

    private String authToken;

    @Value("${cloudcard.api.accessToken}")
    private String persistentAccessToken;

    @Autowired
    RestService restService;

    public TokenService() {

    }

    @PostConstruct
    void init() {
        if (this.persistentAccessTokenIsEmpty()) {
            log.info("Persist. Access Token not set.");
        } else {
            log.info("Persist. Access Token : ..." + persistentAccessToken.substring(3, 8) + "...");
        }
    }

    boolean persistentAccessTokenIsEmpty() {
        return persistentAccessToken == null || persistentAccessToken.isEmpty();
    }

    boolean authTokenIsEmpty() {
        return authToken == null || authToken.isEmpty();
    }

    boolean isConfigured() {
        return !persistentAccessTokenIsEmpty();
    }

    public void login() throws Exception {
        authToken = RemotePhotoUtil.login(apiUrl, persistentAccessToken);
    }

    public void logout() throws Exception {
        if (authTokenIsEmpty()) {
            return;
        }

        RemotePhotoUtil.logout(apiUrl, authToken);

        authToken = null;
    }

    @JsonAnyGetter
    public String getAuthToken() {
        if (authTokenIsEmpty()) {
            try {
                this.login();
            } catch (Exception e) {
                log.error("Error while trying to retrieve token from CloudCard API.", e);
                return null;
            }
        }

        return authToken;
    }

}
