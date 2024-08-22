package com.cloudcard.photoDownloader


import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank


@Component
class OrigoClient {
    // Makes requests to Origo API

    private static final Logger log = LoggerFactory.getLogger(OrigoClient.class)

    @Autowired
    HttpClient httpClient

    @Value('${Origo.eventManagementApi}')
    String eventManagementApi

    @Value('${Origo.mobileIdentitiesApi}')
    String mobileIdentitiesApi

    @Value('${Origo.certIdpApi}')
    String certIdpApi

    @Value('${Origo.organizationId}')
    String organizationId

    @Value('${Origo.contentType}')
    String contentType

    @Value('${Origo.applicationVersion}')
    String applicationVersion

    @Value('${Origo.applicationId}')
    String applicationId

    @Value('${Origo.clientId}')
    String clientId

    @Value('${Origo.clientSecret}')
    String clientSecret

    String accessToken

    boolean isAuthenticated = false

    Map requestHeaders

    boolean authorizeRequests(String token) {
        boolean result = false

        accessToken = token
        isAuthenticated = true
        Map headers = setRequestHeaders(token)

        if (accessToken == token
                && isAuthenticated
                && headers == requestHeaders) result = true

        return result
    }

    Map setRequestHeaders(String token) {
        return requestHeaders = [
                'Authorization'      : "Bearer $token" as String,
                'Content-Type'       : contentType,
                'Application-Version': applicationVersion,
                'Application-ID'     : applicationId
        ]
    }

    @PostConstruct
    init() {
        throwIfBlank(eventManagementApi, "The Origo Event Management API URL must be specified.")
        throwIfBlank(mobileIdentitiesApi, "The Origo Mobile Identities API URL must be specified.")
        throwIfBlank(organizationId, "The Origo organization ID must be specified.")
        throwIfBlank(clientSecret, "The Origo client secret must be specified.")
        throwIfBlank(clientId, "The Origo client ID must be specified.")
        throwIfBlank(contentType, "The Origo content-type header must be specified.")
        throwIfBlank(applicationVersion, "The Origo application version must be specified.")
        throwIfBlank(applicationId, "Your organization's Origo Application ID must be specified.")

        log.info('=================== Initializing Origo Client ===================')
        log.info("          Origo Event Management API URL : $eventManagementApi")
        log.info("         Origo Mobile Identities API URL : $mobileIdentitiesApi")
        log.info("                   Origo organization ID : $organizationId")
        log.info("               Origo content type header : $contentType")
        log.info("               Origo application version : $applicationVersion")
        log.info("                    Origo application ID : $applicationId")

        httpClient.source = this.class.name

    }

    boolean authenticate() {
        // Stores token for future requests

        ResponseWrapper response = requestAccessToken()
        String token = ""
        boolean result = false

        if (response.success) {
            token = response.body.access_token
            result = authorizeRequests(token)
        } else {
            isAuthenticated = false
        }

        return result
    }

    ResponseWrapper requestAccessToken() {
        // https://doc.origo.hidglobal.com/api/authentication/

        String url = "$certIdpApi/authentication/customer/$organizationId/token"
        Map<String, String> headers = ["Content-Type": "application/x-www-form-urlencoded"]
        String body = "client_id=${clientId}&client_secret=${clientSecret}&grant_type=client_credentials"

        ResponseWrapper response = httpClient.makeRequest(Actions.POST.value, url, headers, body)

        httpClient.handleResponseLogging("requestAccessToken", response, "Error while authenticating with Origo.")

        return response
    }

    ResponseWrapper uploadUserPhoto(Photo photo, String fileType) {
        // https://doc.origo.hidglobal.com/api/mobile-identities/#/Photo%20ID/post-customer-organization_id-users-user_id-photo

        String url = "$mobileIdentitiesApi/customer/$organizationId/users/${photo.person.identifier}/photo"

        Map headers = requestHeaders.clone() as Map
        headers['Content-Type'] = "application/vnd.assaabloy.ma.credential-management-2.2+$fileType" as String

        ResponseWrapper response = httpClient.makeRequest(Actions.POST.value, url, headers, null, photo.bytes)

        httpClient.handleResponseLogging("uploadUserPhoto", response)

        return response
    }


    ResponseWrapper accountPhotoApprove(String userId, String id) {
        // https://doc.origo.hidglobal.com/api/mobile-identities/#/Photo%20ID/put-customer-organization_id-users-user_id-photo-photo_id-status

        String url = "$mobileIdentitiesApi/customer/$organizationId/users/${userId}/photo/${id}/status"
        String serializedBody = new ObjectMapper().writeValueAsString([
                status: 'APPROVE'
        ])

        ResponseWrapper response = httpClient.makeRequest(Actions.PUT.value, url, requestHeaders, serializedBody)

        httpClient.handleResponseLogging("accountPhotoApprove", response)

        return response
    }
}