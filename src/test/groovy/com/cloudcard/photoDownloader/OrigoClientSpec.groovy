package com.cloudcard.photoDownloader

import spock.lang.Ignore
import spock.lang.Specification

class OrigoClientSpec extends Specification {
    OrigoClient origoClient

    Map requestHeaders = [
            'Authorization'      : 'Bearer this-is-a-fake-access-token',
            'Content-Type'       : 'application/vnd.assaabloy.ma.credential-management-2.2+json',
            'Application-Version': '2.2',
            'Application-ID'     : 'ORIGO-APPLICATION'
    ]

    def setup() {
        HttpClient httpClient = new HttpClient()
        origoClient = new OrigoClient(httpClient: httpClient)

        origoClient.eventManagementApi = "https://api.mock-event-management.com"
        origoClient.mobileIdentitiesApi = "https://api.mock-mobile-identities.com"
        origoClient.certIdpApi = "https://api.mock-cert-idp.com"
        origoClient.organizationId = "12345"
        origoClient.contentType = "application/vnd.assaabloy.ma.credential-management-2.2+json"
        origoClient.applicationVersion = "2.2"
        origoClient.applicationId = "ORIGO-APPLICATION"
        origoClient.clientId = "67890"
        origoClient.clientSecret = "Password1234"
        origoClient.authorizeRequests "this-is-a-fake-access-token"
    }

    def "Should be initialized"() {
        expect:
        origoClient != null
    }

    def "Should fail to authenticate without stubbing"() {
        expect:
        !origoClient.authenticate()
    }

    def "Should authenticate with mock API"() {
        HttpClient httpClient = Mock()
        String url = "https://api.mock-cert-idp.com/authentication/customer/12345/token"
        String body = "client_id=67890&client_secret=Password1234&grant_type=client_credentials"
        Map headers = ['Content-Type' : 'application/x-www-form-urlencoded']
        Object responseBody = '{"access_token" : "this-is-your-mock-access-token"}'

        httpClient.makeRequest(httpClient.POST, url, headers, body) >> new ResponseWrapper(200, responseBody)
        origoClient.httpClient = httpClient

        expect:
        origoClient.authenticate()
    }

    def "Should upload user photo to mock API"() {
        HttpClient httpClient = Mock()
        String url = "https://api.mock-mobile-identities.com/customer/12345/users/person-1/photo"

        Map headers = requestHeaders.clone() as Map
        headers['Content-Type'] = 'application/vnd.assaabloy.ma.credential-management-2.2+jpg'

        Object responseBody = '{"id" : "new-photo-id"}'

        Person person = new Person()
        person.identifier = "person-1"

        Photo photo = new Photo()
        photo.id = 1234
        photo.person = person
        photo.bytes = new byte[] {1}

        httpClient.makeRequest(httpClient.POST, url, headers, null, photo.bytes) >> new ResponseWrapper(200, responseBody)
        origoClient.httpClient = httpClient

        when:
        ResponseWrapper response = origoClient.uploadUserPhoto(photo, "jpg")

        then:
        response.success
        response.status == 200
        response.body.id == "new-photo-id"
    }

    def "Should approve user photo in mock API"() {
        HttpClient httpClient = Mock()
        String url = "https://api.mock-mobile-identities.com/customer/12345/users/person-1/photo/1234/status"

        String body = '{"status":"APPROVE"}'

        Person person = new Person()
        person.identifier = "person-1"

        Photo photo = new Photo()
        photo.id = 1234
        photo.person = person

        httpClient.makeRequest(httpClient.PUT, url, requestHeaders, body) >> new ResponseWrapper(200)
        origoClient.httpClient = httpClient

        when:
        ResponseWrapper response = origoClient.accountPhotoApprove(person.identifier, "1234")

        then:
        response.success
        response.status == 200
    }


}