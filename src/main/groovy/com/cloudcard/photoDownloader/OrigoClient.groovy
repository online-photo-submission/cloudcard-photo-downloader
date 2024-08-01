package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.exceptions.UnirestException
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank


@Component
@ConditionalOnProperty(name = 'Origo.useOrigo', havingValue = 'true')
class OrigoClient extends HttpClient {
    // Makes requests to Origo API
    private static final Logger log = LoggerFactory.getLogger(OrigoClient.class)

    @Value('${Origo.eventManagementApi}')
    private String eventManagementApi

    @Value('${Origo.callbackRegistrationApi}')
    private String callbackRegistrationApi

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

    @Value('${Origo.filterSet}')
    String filterSetString

    private List<String> filterSet = []

    List<String> getFilterSet() {
        return filterSet
    }

    boolean isAuthenticated = accessToken ? true : false

    @Autowired
    Utils utils

    String lastDateTo = ""

    String lastDateFrom = "2024-07-31T20:42:59Z"

    private Map requestHeaders

    void setToken(String token) {
        accessToken = token
        log.info("Saving token: $token")
        isAuthenticated = true
        setRequestHeaders(token)
    }

    void setRequestHeaders(String token) {
        requestHeaders = [
                'Authorization'      : 'Bearer ' + token,
                'Content-Type'       : contentType,
                'Application-Version': applicationVersion,
                'Application-ID'     : applicationId
        ]
    }

    @PostConstruct
    init() {
        throwIfBlank(eventManagementApi, "The Origo Event Management API URL must be specified.")
        throwIfBlank(callbackRegistrationApi, "The Origo Callback Registration API URL must be specified.")
        throwIfBlank(mobileIdentitiesApi, "The Origo Mobile Identities API URL must be specified.")
        throwIfBlank(organizationId, "The Origo organization ID must be specified.")
        throwIfBlank(contentType, "The Origo content-type header must be specified.")
        throwIfBlank(applicationVersion, "The Origo application version must be specified.")
        throwIfBlank(applicationId, "Your organization's Origo Application ID must be specified.")
        throwIfBlank(filterSetString, "A list of Origo event filters must be specified.")

        log.info('=================== Initializing Origo Client ===================')
        log.info("          Origo Event Management API URL : $eventManagementApi")
        log.info("     Origo Callback Registration API URL : $callbackRegistrationApi")
        log.info("         Origo Mobile Identities API URL : $mobileIdentitiesApi")
        log.info("                   Origo organization ID : $organizationId")
        log.info("               Origo content type header : $contentType")
        log.info("               Origo application version : $applicationVersion")
        log.info("                    Origo application ID : $applicationId")
        log.info("                     Origo event filters : $filterSetString")

        if (accessToken) {
            setToken(accessToken)
        } else {
            log.warn("ORIGOCLIENT: No access token  present during initialization.")
        }

        filterSet = filterSetString.split(", ")

        this.implementingClass = "OrigoClient"
    }

    ResponseWrapper authenticate() {
        String url = "$certIdpApi/authentication/customer/$organizationId/token"
        Map<String, String> headers = ["Content-Type": "application/x-www-form-urlencoded"]
        String body = "client_id=${clientId}&client_secret=${clientSecret}&grant_type=client_credentials"

        ResponseWrapper response = makeRequest("authenticate", "post", url, headers, body)
        return response
    }

    ResponseWrapper createFilter(List<String> filters) {
        // Filters what kinds of events this instance will look for in other calls. See documentation for options: https://doc.origo.hidglobal.com/api/events-callbacks/#/Events/post_organization__organization_id__events_filter

        String serializedBody = new ObjectMapper().writeValueAsString([
                filterSet: filters
        ])

        Map headers = [
                'Authorization' : 'Bearer ' + accessToken,
                'Content-Type' : 'application/json',
                'Application-Id' : applicationId
        ]

        log.info(serializedBody)

        String url = "$eventManagementApi/organization/$organizationId/events/filter"

        ResponseWrapper response = makeRequest("createFilter", "post", url, headers, serializedBody)

        return response
    }

    ResponseWrapper listEvents(String dateFrom = "", String dateTo = "", String filterId = "", String callbackStatus = "") {

        if (!dateFrom || dateFrom == lastDateFrom) dateFrom = lastDateFrom
        if (!dateTo) dateTo = utils.nowAsIsoFormat()

        String url = "$eventManagementApi/organization/$organizationId/events?dateFrom=$dateFrom&dateTo=$dateTo${!filterId ? "" : "&filterId=$filterId"}${!callbackStatus ? "" : "&callbackStatus=$callbackStatus"}".toString()

        ResponseWrapper response  = makeRequest("listEvents", "get", url, requestHeaders)

        return response
    }

    ResponseWrapper listFilters() {

        String url = "$eventManagementApi/organization/$organizationId/events/filter"

        ResponseWrapper response = makeRequest("listFilters", "get", url, requestHeaders)

        return response
    }

//    ResponseWrapper uploadUserPhoto(String userId, Photo photo) {
//        // posts photo to User's Origo profile: https://doc.origo.hidglobal.com/api/mobile-identities/#/Photo%20ID/post-customer-organization_id-users-user_id-photo
//
//        String serializedBody = new ObjectMapper().writeValueAsString(photo.bytes)
//
//        HttpResponse<String> response
//        ResponseWrapper response
//
//        try {
//            response = Unirest.post(mobileIdentitiesApi + "/customer/$organizationId/users/$userId/photo")
//                    .headers(requestHeaders)
//                    .body(serializedBody)
//                    .asString()
//
//            log.info("Response: $response")
//            response = new ResponseWrapper(response)
//
//        } catch (UnirestException e) { // ?
//            log.error(e.message)
//            response = new ResponseWrapper(e)
//        }
//
//        return response
//    }

//    ResponseWrapper getFilterById() {
//        // checks for current filters. Conditionally calls create filter
//
//        HttpResponse<String> response
//        ResponseWrapper response
//
//        try {
//            response = Unirest.get(eventManagementApi + "/organization/$organizationId/events/filter/$filterId")
//                    .headers(requestHeaders)
//                    .asString()
//
//            log.info("Response: $response")
//            response = new ResponseWrapper(response)
//
//        } catch (UnirestException e) { // ?
//            log.error(e.message)
//            response = new ResponseWrapper(e)
//        }
//
//        return response
//    }
//
//    ResponseWrapper updatePhotoApprovalStatus(String userId, String photoId, boolean status) {
//        // approves photo in origo after upload. REQUIRED for photo credential to be used.
//
//        String serializedBody = new ObjectMapper().writeValueAsString([
//                status: status ? 'APPROVE' : 'REJECT'
//        ])
//
//        HttpResponse<String> response
//        ResponseWrapper response
//
//        try {
//            response = Unirest.put(mobileIdentitiesApi + "/customer/$organizationId/users/$userId/photo/$photoId/status")
//                    .headers(requestHeaders)
//                    .body(serializedBody)
//                    .asString()
//
//            log.info("Response: $response")
//            response = new ResponseWrapper(response)
//
//        } catch (UnirestException e) { // ?
//            log.error(e.message)
//            response = new ResponseWrapper(e)
//        }
//
//        return response
//    }


}