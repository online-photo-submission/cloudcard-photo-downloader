package com.cloudcard.photoDownloader.integrations

import com.cloudcard.photoDownloader.HttpClient
import com.cloudcard.photoDownloader.Person
import com.cloudcard.photoDownloader.ResponseWrapper
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

@Component
class CloudCardClient extends HttpClient {
    private static final Logger log = LoggerFactory.getLogger(CloudCardClient.class)

    boolean isAuthenticated = false

    private Map requestHeaders

    @Value('${cloudcard.api.test-url}')
    // Change this
    private String cloudCardApi

    @Value('${cloudcard.api.accessToken}')
    private String persistentAccessToken

    @Value('${cloudcard.api.sessionToken}')
    private String sessionToken

    void setToken(String token) {
        sessionToken = token
        log.info("Saving token: $token")
        isAuthenticated = true
        setRequestHeaders(token)
    }

    void setRequestHeaders(String token) {
        requestHeaders = [
                'Content-Type': 'application/json',
                'X-Auth-Token': token,
                'Accept'      : 'application/json'
        ]
    }

    @PostConstruct
    init() {
        throwIfBlank(cloudCardApi, "A CloudCard API url must be specified.")
        throwIfBlank(persistentAccessToken, "A CloudCard access token must be specified.")

        log.info('=================== Initializing CloudCard Client ===================')
        log.info("                       CloudCard API url : $cloudCardApi")
        log.info("       CloudCard Persistent Access Token : ${persistentAccessToken ? "Present" : "Missing"}")

        if (!persistentAccessToken) {
            log.warn("CLOUDCARDCLIENT: No persistent access token present during initialization.")
        }

        if (sessionToken) {
            setToken(sessionToken)
        }

        this.implementingClass = "CloudCardClient"

    }

    ResponseWrapper authenticate() {
        String url = "$cloudCardApi/authentication-token"
        String serializedBody = new ObjectMapper().writeValueAsString([
                'persistentAccessToken': persistentAccessToken
        ])

        Map headers = [
                'Content-Type': 'application/json',
                'Accept'      : 'application/json'
        ]

        ResponseWrapper response = makeRequest("authenticate", "post", url, headers, serializedBody)

        return response
    }

    ResponseWrapper createPerson(Person person) {
        String url = "$cloudCardApi/person?renderResource=true"
        String serializedBody = new ObjectMapper().writeValueAsString(person)
        log.info(serializedBody)
        Map headers = requestHeaders

        ResponseWrapper response = makeRequest("createPerson", "post", url, headers, serializedBody)

        return response
    }
}