package com.cloudcard.photoDownloader


import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank


@Component
class OrigoClient extends HttpClient {
    // Makes requests to Origo API

    private static final Logger log = LoggerFactory.getLogger(OrigoClient.class)

    @Value('${Origo.eventManagementApi}')
    private String eventManagementApi

    @Value('${Origo.mobileIdentitiesApi}')
    private String mobileIdentitiesApi

    @Value('${Origo.certIdpApi}')
    private String certIdpApi

    @Value('${Origo.organizationId}')
    private String organizationId

    @Value('${Origo.accessToken}')
    private String accessToken

    @Value('${Origo.contentType}')
    private String contentType

    @Value('${Origo.applicationVersion}')
    private String applicationVersion

    @Value('${Origo.applicationId}')
    private String applicationId

    @Value('${Origo.clientId}')
    private String clientId

    @Value('${Origo.clientSecret}')
    private String clientSecret

    boolean isAuthenticated = accessToken ? true : false

    private Map requestHeaders

    void authorizeRequests(String token) {
        accessToken = token
        isAuthenticated = true
        setRequestHeaders(token)
    }

    void setRequestHeaders(String token) {
        requestHeaders = [
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

        if (accessToken) {
            authorizeRequests(accessToken)
        } else {
            log.info("No access token present during initialization.")
        }

        this.extendingClass = this.class.name
    }

    boolean authenticate() {
        // Stores token for future requests

        ResponseWrapper response = requestAccessToken()
        String token = ""
        boolean result = false

        if (response.success) {
            token = response.body.access_token
            authorizeRequests(token)
            result = true
        }

        return result
    }

    ResponseWrapper requestAccessToken() {
        // https://doc.origo.hidglobal.com/api/authentication/

        String url = "$certIdpApi/authentication/customer/$organizationId/token"
        Map<String, String> headers = ["Content-Type": "application/x-www-form-urlencoded"]
        String body = "client_id=${clientId}&client_secret=${clientSecret}&grant_type=client_credentials"

        ResponseWrapper response = makeRequest("post", url, headers, body)

        handleResponseLogging("requestAccessToken", response, "Error while authenticating with Origo.")

        return response
    }

    ResponseWrapper uploadUserPhoto(Photo photo, String fileType) {
        // https://doc.origo.hidglobal.com/api/mobile-identities/#/Photo%20ID/post-customer-organization_id-users-user_id-photo

        String url = "$mobileIdentitiesApi/customer/$organizationId/users/${photo.person.identifier}/photo"

        Map headers = requestHeaders.clone() as Map
        headers['Content-Type'] = "application/vnd.assaabloy.ma.credential-management-2.2+$fileType" as String

        ResponseWrapper response = makeRequest("post", url, headers, null, photo.bytes)

        handleResponseLogging("uploadUserPhoto", response)

        return response
    }


    ResponseWrapper accountPhotoApprove(Photo photo, String id) {
        // https://doc.origo.hidglobal.com/api/mobile-identities/#/Photo%20ID/put-customer-organization_id-users-user_id-photo-photo_id-status

        String url = "$mobileIdentitiesApi/customer/$organizationId/users/${photo.person.identifier}/photo/${id}/status"
        String serializedBody = new ObjectMapper().writeValueAsString([
                status: 'APPROVE'
        ])

        ResponseWrapper response = makeRequest("put", url, requestHeaders, serializedBody)

        handleResponseLogging("accountPhotoApprove", response)

        return response
    }
}