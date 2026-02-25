package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank;

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
        String url =  apiUrl + "/authenticationTokens";
        HttpResponse<String> response = Unirest.post(url).headers(standardHeaders(false)).body("{\"persistentAccessToken\":\"" + persistentAccessToken + "\"}").asString();

         if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when retrieving accessToken.");
            return;
        }

        AuthenticationToken token = new ObjectMapper().readValue(response.getBody(), new TypeReference<AuthenticationToken>(){
        });

        authToken = token.getTokenValue();
    }

    public void logout() throws Exception {
        if (authTokenIsEmpty()) {
            return;
        }

        String url =  apiUrl + "/people/me/logout";
        HttpResponse<String> response = Unirest.post(url).headers(standardHeaders(true)).body("{\"authenticationToken\":\"" + authToken + "\"}").asString();

        if (response.getStatus() != 204) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when logging out accessToken.");
        }
    }

    private Map<String, String> standardHeaders(boolean includeToken) {

        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("Content-Type", "application/json");
        if (includeToken) headers.put("X-Auth-Token", getAuthToken());
        return headers;
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
