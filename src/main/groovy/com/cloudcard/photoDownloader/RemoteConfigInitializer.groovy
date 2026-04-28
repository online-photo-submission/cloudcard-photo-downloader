package com.cloudcard.photoDownloader

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import kong.unirest.core.HttpResponse
import kong.unirest.core.Unirest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class RemoteConfigInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger log = LoggerFactory.getLogger(RemoteConfigInitializer.class)
    ObjectMapper jacksonMapper = new ObjectMapper();
    private String authToken;

    @Override
    void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment env = context.environment
        jacksonMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 1. Get seed info from local env (PAT and API URL)
        String pat = env.getProperty("cloudcard.api.accessToken")
        String apiUrl = env.getProperty("cloudcard.api.url")
        String integrationName = env.getProperty("cloudcard.integration.name")

        // 2. Fetch the JSON from your API
        Map<String, Object> remoteProperties = fetchRemoteConfig(apiUrl, pat, integrationName)

        // 3. Inject into the environment at the highest priority
        env.getPropertySources().addFirst(new MapPropertySource("remoteApiConfig", remoteProperties));
    }

    private Map<String, Object> fetchRemoteConfig(String apiUrl, String pat, String integrationName) {
        authToken = login(apiUrl, pat);
        log.info("Successfully retrieved auth token from CloudCard API. ${authToken}");
//       TODO: Make the actual API call to fetch the config using authToken

        String url =  apiUrl + "/integrations/$integrationName?findBy=name";
        HttpResponse<String> response = Unirest.get(url).headers(standardHeaders(true)).asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when retrieving integration.");
            return;
        }

        Integration integration = new ObjectMapper().readValue(response.getBody(), new TypeReference<Integration>(){});

        return integration.integrationConfigs.collectEntries { [it.propertyName, it.propertyValue] }
    }
//        TODO: This duplicates code from the token service. Consider refactoring into a utility class or something.
    private String login(String apiUrl, String persistentAccessToken) throws Exception {
        String url =  apiUrl + "/authenticationTokens";
        HttpResponse<String> response = Unirest.post(url).headers(standardHeaders(false)).body("{\"persistentAccessToken\":\"" + persistentAccessToken + "\"}").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when retrieving accessToken.");
            return;
        }

        AuthenticationToken token = new ObjectMapper().readValue(response.getBody(), new TypeReference<AuthenticationToken>(){
        });

       return token.getTokenValue();
    }

    private Map<String, String> standardHeaders(boolean includeToken) {

        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("Content-Type", "application/json");
        if (includeToken) headers.put("X-Auth-Token", authToken);
        return headers;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Integration {
    String name
    String integrationType
    List<IntegrationConfig> integrationConfigs
}

@JsonIgnoreProperties(ignoreUnknown = true)
class IntegrationConfig {
    String propertyName
    String propertyValue
}