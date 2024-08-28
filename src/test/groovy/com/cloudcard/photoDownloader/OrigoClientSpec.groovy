package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import spock.lang.Specification
import spock.lang.Unroll

class OrigoClientSpec extends Specification {
    OrigoClient origoClient

    String accessToken = "this-is-the-original-access-token"

    def setup() {
        HttpClient httpClient = Mock()
        UnirestWrapper unirestWrapper = Mock()

        origoClient = new OrigoClient(httpClient: httpClient, unirestWrapper: unirestWrapper)

        origoClient.eventManagementApi = "https://api.mock-event-management.com"
        origoClient.mobileIdentitiesApi = "https://api.mock-mobile-identities.com"
        origoClient.certIdpApi = "https://api.mock-cert-idp.com"
        origoClient.organizationId = "12345"
        origoClient.contentType = "application/vnd.assaabloy.ma.credential-management-2.2+json"
        origoClient.applicationVersion = "2.2"
        origoClient.applicationId = "ORIGO-APPLICATION"
        origoClient.clientId = "67890"
        origoClient.clientSecret = "Password1234"
    }

    def "Should be initialized"() {
        expect:
        origoClient != null
    }

    def "should instantiate unirest wrapper"() {
        expect:
        origoClient.unirestWrapper != null
    }

    def "should instantiate http client"() {
        expect:
        origoClient.httpClient != null
    }

    def "authenticate should hydrate properties with token"() {
        given:
        Object responseBody = '{"access_token" : "this-is-your-mock-access-token"}'
        ResponseWrapper responseWithToken = new ResponseWrapper(200, responseBody)
        origoClient = Spy(OrigoClient) {
            it.isAuthenticated = false
            it.requestHeaders = null
        }

        when:
        origoClient.authenticate(responseWithToken)

        then:
        origoClient.isAuthenticated
        origoClient.requestHeaders == [
                'Authorization'      : 'Bearer this-is-your-mock-access-token',
                'Content-Type'       : origoClient.contentType,
                'Application-Version': origoClient.applicationVersion,
                'Application-ID'     : origoClient.applicationId
        ]
    }

    @Unroll
    def "requestAccessToken should properly handle different Http responses - status: #status, body: #body"() {
        given:
        HttpClient httpClient = Mock()

        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.post(_, _, _) >> httpResponse

        origoClient.httpClient = httpClient
        origoClient.unirestWrapper = unirestWrapper

        when:
        ResponseWrapper response = origoClient.requestAccessToken()

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)
        1 * httpClient.handleResponseLogging(_, _, _)

        where:

        status | body
        200    | "{'access_token': 'some-access-token'"
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null
    }

    def "Should fail to authenticate with bad request"() {
        given:
        origoClient = Spy(OrigoClient) {
            it.requestAccessToken() >> new ResponseWrapper(400)
        }

        when:
        origoClient.authenticate(origoClient.requestAccessToken())

        then:
        !origoClient.isAuthenticated
        origoClient.accessToken == null
    }

    def "Should authenticate with good request"() {
        given:
        Object responseBody = '{"access_token" : "this-is-your-mock-access-token"}'

        origoClient = Spy(OrigoClient) {
            it.requestAccessToken() >> new ResponseWrapper(200, responseBody)
        }

        when:
        origoClient.authenticate(origoClient.requestAccessToken())

        then:
        origoClient.isAuthenticated
        origoClient.accessToken == "this-is-your-mock-access-token"
    }

    def "makeAuthenticatedRequest should authenticate if !isAuthenticated"() {
        given:
        Object responseBody = '{"access_token" : "this-is-your-mock-access-token"}'
        ResponseWrapper responseWithToken = new ResponseWrapper(200, responseBody)

        origoClient = Spy(OrigoClient) {
            it.isAuthenticated = false
            it.requestAccessToken() >> responseWithToken
            it.authenticate(it.requestAccessToken()) >> {
                origoClient.isAuthenticated = true
                return true
            }

        }

        when:
        origoClient.makeAuthenticatedRequest { new ResponseWrapper(200, "Some information") }

        then:
        1 * origoClient.authenticate(responseWithToken)
        origoClient.isAuthenticated

    }

    def "makeAuthenticatedRequest should skip authentication if isAuthenticated == true"() {
        given:
        Object responseBody = '{"access_token" : "this-is-your-mock-access-token"}'
        ResponseWrapper responseWithToken = new ResponseWrapper(200, responseBody)

        origoClient = Spy(OrigoClient) {
            it.isAuthenticated = true
        }
        origoClient.requestAccessToken() >> responseWithToken

        when:
        origoClient.makeAuthenticatedRequest { new ResponseWrapper(200, "Some info") }

        then:
        0 * origoClient.authenticate(_)
        origoClient.isAuthenticated
    }

    def "makeAuthenticatedRequest should return response with AuthException if authentication fails"() {
        given:
        ResponseWrapper responseWithToken = new ResponseWrapper(400, "Bad Request")

        origoClient = Spy(OrigoClient) {
            it.isAuthenticated = false
            it.requestAccessToken() >> responseWithToken
        }

        when:
        ResponseWrapper response = origoClient.makeAuthenticatedRequest { new ResponseWrapper(200, "Some info") }

        then:
        1 * origoClient.authenticate(_)
        !origoClient.isAuthenticated
        response.exception != null
        !response.success

    }

    @Unroll
    def "uploadUserPhoto should properly handle different Http responses. - body: #body status: #status"() {
        given:

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[]{1})

        HttpClient httpClient = Mock()

        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.post(_, _, _) >> httpResponse

        origoClient.httpClient = httpClient
        origoClient.unirestWrapper = unirestWrapper
        origoClient.requestHeaders = [
                'Authorization'      : "Bearer $accessToken" as String,
                'Content-Type'       : origoClient.contentType,
                'Application-Version': origoClient.applicationVersion,
                'Application-ID'     : origoClient.applicationId
        ]

        when:
        ResponseWrapper response = origoClient.uploadUserPhoto(photo, "jpg")

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)
        1 * httpClient.handleResponseLogging(_, _)

        where:

        status | body
        200    | '{"id" : "new-photo-id"}'
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null

    }

    @Unroll
    def "accountPhotoApprove should properly handle different Http responses. - body: #body status: #status"() {
        given:

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[]{1})

        HttpClient httpClient = Mock()

        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.put(_, _, _) >> httpResponse

        origoClient.httpClient = httpClient
        origoClient.unirestWrapper = unirestWrapper
        origoClient.requestHeaders = [
                'Authorization'      : "Bearer $accessToken" as String,
                'Content-Type'       : origoClient.contentType,
                'Application-Version': origoClient.applicationVersion,
                'Application-ID'     : origoClient.applicationId
        ]

        when:
        ResponseWrapper response = origoClient.accountPhotoApprove(photo.person.identifier, "12345")

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)
        1 * httpClient.handleResponseLogging(_, _)

        where:

        status | body
        200    | '{"body" : "Photo approved"}'
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null

    }

    @Unroll
    def "getUserDetails should properly handle different Http responses. - body: #body status: #status"() {
        given:

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[]{1})

        HttpClient httpClient = Mock()

        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.get(_, _) >> httpResponse

        origoClient.httpClient = httpClient
        origoClient.unirestWrapper = unirestWrapper
        origoClient.requestHeaders = [
                'Authorization'      : "Bearer $accessToken" as String,
                'Content-Type'       : origoClient.contentType,
                'Application-Version': origoClient.applicationVersion,
                'Application-ID'     : origoClient.applicationId
        ]

        when:
        ResponseWrapper response = origoClient.getUserDetails("12345")

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)
        1 * httpClient.handleResponseLogging(_, _)

        where:

        status | body
        200    | '{"body" : "some user details"}'
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null

    }

    @Unroll
    def "deletePhoto should properly handle different Http responses. - body: #body status: #status"() {
        given:
        HttpClient httpClient = Mock()

        HttpResponse<String> httpResponse = Mock()
        httpResponse.getStatus() >> status
        httpResponse.getBody() >> body

        UnirestWrapper unirestWrapper = Mock()
        unirestWrapper.delete(_, _) >> httpResponse

        origoClient.httpClient = httpClient
        origoClient.unirestWrapper = unirestWrapper
        origoClient.requestHeaders = [
                'Authorization'      : "Bearer $accessToken" as String,
                'Content-Type'       : origoClient.contentType,
                'Application-Version': origoClient.applicationVersion,
                'Application-ID'     : origoClient.applicationId
        ]

        when:
        ResponseWrapper response = origoClient.deletePhoto("12345", "6789")

        then:
        response.status == status
        response.body == ResponseWrapper.parseBody(body)
        1 * httpClient.handleResponseLogging(_, _)

        where:

        status | body
        204    | ''
        400    | '{"body" : "BADREQUEST"}'
        401    | '{"body" : "UNAUTHORIZED"}'
        500    | '{"body" : "SERVERERROR"}'
        0      | null

    }

}