package com.cloudcard.photoDownloader

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import kong.unirest.core.HttpResponse
import kong.unirest.core.Unirest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class RemotePhotoUtil {
    private static final Logger log = LoggerFactory.getLogger(RemotePhotoUtil.class)

    /*
        * Logs in to the CloudCard API using a persistent access token and retrieves an authentication token.
        * @param apiUrl
        * @param persistentAccessToken
        * @return The authentication token value if login is successful, null otherwise.
     */
    static String login(String apiUrl, String persistentAccessToken) throws Exception {
        String url =  apiUrl + "/authenticationTokens"
        HttpResponse<String> response = Unirest.post(url).headers(standardHeaders()).body("{\"persistentAccessToken\":\"" + persistentAccessToken + "\"}").asString()

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when retrieving accessToken.")
            return
        }

        AuthenticationToken token = new ObjectMapper().readValue(response.getBody(), new TypeReference<AuthenticationToken>(){
        })

        return token.getTokenValue()
    }

    /*
        * Logs out of the CloudCard API by invalidating the provided authentication token.
        * @param apiUrl The base URL of the CloudCard API.
        * @param authToken The authentication token to invalidate.
     */
    static void logout(String apiUrl, String authToken) throws Exception {
        if (!authToken) {
            return
        }

        try {
            String url = apiUrl + "/people/me/logout"
            HttpResponse<String> response = Unirest.post(url).headers(standardHeaders(authToken)).body("{\"authenticationToken\":\"" + authToken + "\"}").asString()

            if (response.getStatus() != 204) {
                log.error("Status " + response.getStatus() + " returned from CloudCard API when logging out accessToken.")
            }

        } catch (Exception e) {
            log.error("Exception thrown when logging out accessToken: " + e.getMessage())
        }
    }

    /*
        * Retrieves the integration configuration from the CloudCard API for a given integration name.
        * @param apiUrl
        * @param integrationName - the name of the integration to retrieve.
        * @param authToken
        * @return An Integration object containing the configuration details if successful, null otherwise.
     */
    static Integration getRemoteConfig(String apiUrl, String integrationName, String authToken) throws Exception {
        String url =  apiUrl + "/integrations/$integrationName?findBy=name"
        HttpResponse<String> response = Unirest.get(url).headers(standardHeaders(authToken)).asString()

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + " returned from CloudCard API when retrieving integration.")
            return null
        }

        return new ObjectMapper().readValue(response.getBody(), new TypeReference<Integration>(){})
    }

    private static Map<String, String> standardHeaders(String authToken = null) {
        Map<String, String> headers = new HashMap<>()
        headers.put("accept", "application/json")
        headers.put("Content-Type", "application/json")
        if (authToken) headers.put("X-Auth-Token", authToken)
        return headers
    }
}
