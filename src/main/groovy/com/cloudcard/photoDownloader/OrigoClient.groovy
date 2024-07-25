package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.exceptions.UnirestException

import com.mashape.unirest.http.Unirest
import jakarta.annotation.PostConstruct
import org.apache.http.HttpException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank


@Component
@ConditionalOnProperty(name = 'Origo.useOrigo', havingValue = 'true')
class OrigoClient {
    // Makes requests to Origo API
    private static final Logger log = LoggerFactory.getLogger(OrigoClient.class);

    @Value('${Origo.eventManagementApi}')
    private String eventManagementApi

    @Value('${Origo.callbackRegistrationApi}')
    private String callbackRegistrationApi

    @Value('${Origo.mobileIdentitiesApi}')
    private String mobileIdentitiesApi

    @Value('${Origo.customerId}')
    private String customerId

    @Value('${Origo.organizationId}')
    private String organizationId

    @Value('${Origo.accessToken}')
    private accessToken

    @Value('${Origo.tokenType} ${Origo.accessToken}')
    private String authorization

    @Value('${Origo.contentType}')
    private String contentType

    @Value('${Origo.applicationVersion}')
    private String applicationVersion

    @Value('${Origo.applicationId}')
    private String applicationId

    @Value('${Origo.filterSet}')
    String filterSet

    // String callbackUrl --> May not need property

    Map requestHeaders

    @PostConstruct
    init() {
        throwIfBlank(eventManagementApi, "The Origo Event Management API URL must be specified.")
        throwIfBlank(callbackRegistrationApi, "The Origo Callback Registration API URL must be specified.")
        throwIfBlank(mobileIdentitiesApi, "The Origo Mobile Identities API URL must be specified.")
        throwIfBlank(customerId, "The Origo customer ID must be specified.")
        throwIfBlank(organizationId, "The Origo organization ID must be specified.")
        throwIfBlank(authorization, "The Origo authorization string must be specified.")
        throwIfBlank(accessToken, "An Origo access token must be provided.")
        throwIfBlank(contentType, "The Origo content-type header must be specified.")
        throwIfBlank(applicationVersion, "The Origo application version must be specified.")
        throwIfBlank(applicationId, "Your organization's Origo Application ID must be specified.")
        throwIfBlank(filterSet, "A list of Origo event filters must be specified.")

        log.info('=================== Initializing Origo Client ===================')
        log.info("          Origo Event Management API URL : $eventManagementApi")
        log.info("     Origo Callback Registration API URL : $callbackRegistrationApi")
        log.info("         Origo Mobile Identities API URL : $mobileIdentitiesApi")
        log.info("                       Origo customer ID : $customerId")
        log.info("                   Origo organization ID : $organizationId")
        log.info("              Origo authorization string : ${!authorization ? 'Missing' : 'Provided'}")
        log.info("               Origo content type header : $contentType")
        log.info("               Origo application version : $applicationVersion")
        log.info("                    Origo application ID : $applicationId")
        log.info("                     Origo event filters : $filterSet")

        requestHeaders = [
                'Authorization'      : authorization,
                'Content-Type'       : contentType,
                'Application-Version': applicationVersion,
                'Application-ID'     : applicationId
        ]
    }

//    authenticate() {
//        // Authenticates session by validating secret token. **May not be necessary
//    }

    HttpActionResult listEvents(String dateFrom, String dateTo, String filterId = "", String callbackStatus = "") {

        HttpResponse<String> response
        HttpActionResult httpActionResult = new HttpActionResult()

        String url = "$eventManagementApi/organization/$organizationId/events?dateFrom=$dateFrom&dateTo=$dateTo${!filterId ?: "&filterId=$filterId"}${!callbackStatus ?: "callbackStatus=$callbackStatus"}"

        try {
            response = Unirest.get(url)
                    .headers(requestHeaders)
                    .asString()

            log.info("Response: $response")
            OrigoResponse origoResponse = new OrigoResponse(response)
            httpActionResult.result = origoResponse

        } catch (UnirestException e) {
            log.error(e.message)
            httpActionResult.result = e
        }

        return httpActionResult

    }

    HttpActionResult createCallbackSubscription(String filterId, String callbackUrl) {
        // subscribes application to Origo organization-specific events

        String serializedBody = new ObjectMapper().writeValueAsString([
                url       : "$callbackUrl",
                filterId  : "$filterId",
                httpHeader: "Authorization",
                secret    : "$authorization"
        ])

        HttpResponse<String> response
        HttpActionResult httpActionResult = new HttpActionResult()

        try {
            response = Unirest.post(eventManagementApi + "/organization/$organizationId/callback")
                    .headers(requestHeaders)
                    .body(serializedBody)
                    .asString()

            log.info("Response: $response")
            OrigoResponse origoResponse = new OrigoResponse(response)
            httpActionResult.result = origoResponse

        } catch (HttpException e) { // ?
            log.error(e.message)
            httpActionResult.result = e
        }

        return httpActionResult
    }

    HttpActionResult uploadUserPhoto(String userId, Photo photo) {
        // posts photo to User's Origo profile: https://doc.origo.hidglobal.com/api/mobile-identities/#/Photo%20ID/post-customer-organization_id-users-user_id-photo

        String serializedBody = new ObjectMapper().writeValueAsString(photo.bytes)

        HttpResponse<String> response
        HttpActionResult httpActionResult = new HttpActionResult()

        try {
            response = Unirest.post(mobileIdentitiesApi + "/customer/$organizationId/users/$userId/photo")
                    .headers(requestHeaders)
                    .body(serializedBody)
                    .asString()

            log.info("Response: $response")
            OrigoResponse origoResponse = new OrigoResponse(response)
            httpActionResult.result = origoResponse

        } catch (HttpException e) { // ?
            log.error(e.message)
            httpActionResult.result = e
        }

        return httpActionResult
    }

    HttpActionResult listCallbackSubscriptions() {

        HttpResponse<String> response
        HttpActionResult httpActionResult = new HttpActionResult()

        try {
            response = Unirest.get(eventManagementApi + "/organization/$organizationId/callback")
                    .headers(requestHeaders)
                    .asString()

            log.info("Response: $response")
            OrigoResponse origoResponse = new OrigoResponse(response)
            httpActionResult.result = origoResponse

        } catch (UnirestException e) {
            log.error(e.message)
            httpActionResult.result = e
        }

        return httpActionResult

    }

    HttpActionResult listFilters() {
        HttpResponse<String> response
        HttpActionResult httpActionResult = new HttpActionResult()

        try {
            response = Unirest.get(eventManagementApi + "/organization/$organizationId/events/filter")
                    .headers(requestHeaders)
                    .asString()

            OrigoResponse origoResponse = new OrigoResponse(response)
            httpActionResult.result = origoResponse

        } catch (UnirestException e) {
            log.error(e.message)
            httpActionResult.result = e
        }

        return httpActionResult

    }

    HttpActionResult getFilterById() {
        // checks for current filters. Conditionally calls create filter

        HttpResponse<String> response
        HttpActionResult httpActionResult = new HttpActionResult()

        try {
            response = Unirest.get(eventManagementApi + "/organization/$organizationId/events/filter/$filterId")
                    .headers(requestHeaders)
                    .asString()

            log.info("Response: $response")
            OrigoResponse origoResponse = new OrigoResponse(response)
            httpActionResult.result = origoResponse

        } catch (UnirestException e) {
            log.error(e.message)
            httpActionResult.result = e
        }

        return httpActionResult
    }

    HttpActionResult createFilter(List<String> filters) {
        // Filters what kinds of events this instance of the application will be subscribed to. See documentation for options: https://doc.origo.hidglobal.com/api/events-callbacks/#/Events/post_organization__organization_id__events_filter

        String serializedBody = new ObjectMapper().writeValueAsString([
                filterSet: filters
        ])

        HttpResponse<String> response
        HttpActionResult httpActionResult = new HttpActionResult()

        try {
            response = Unirest.post(eventManagementApi + "/organization/$organizationId/events/filter")
                    .headers(requestHeaders)
                    .body(serializedBody)
                    .asString()

            log.info("Response: $response")
            OrigoResponse origoResponse = new OrigoResponse(response)
            httpActionResult.result = origoResponse

        } catch (HttpException e) { // ?
            log.error(e.message)
            httpActionResult.result = e
        }

        return httpActionResult
    }

    HttpActionResult updatePhotoApprovalStatus(String userId, String photoId, boolean status) {
        // approves photo in origo after upload. REQUIRED for photo credential to be used.

        String serializedBody = new ObjectMapper().writeValueAsString([
                status: status ? 'APPROVE' : 'REJECT'
        ])

        HttpResponse<String> response
        HttpActionResult httpActionResult = new HttpActionResult()

        try {
            response = Unirest.put(mobileIdentitiesApi + "/customer/$organizationId/users/$userId/photo/$photoId/status")
                    .headers(requestHeaders)
                    .body(serializedBody)
                    .asString()

            log.info("Response: $response")
            OrigoResponse origoResponse = new OrigoResponse(response)
            httpActionResult.result = origoResponse

        } catch (HttpException e) { // ?
            log.error(e.message)
            httpActionResult.result = e
        }

        return httpActionResult
    }

//    storePersonData() {
//        // stores 'customFields' information in Origo employee record
//    }
}

class OrigoResponse extends ThirdPartyResponse {

    OrigoResponse(HttpResponse<String> response) {
        super(response)
    }
}
