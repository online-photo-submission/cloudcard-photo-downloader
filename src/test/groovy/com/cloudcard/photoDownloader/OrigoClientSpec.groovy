package com.cloudcard.photoDownloader

import spock.lang.Specification

class OrigoClientSpec extends Specification {
    OrigoClient origoClient

    HttpClient httpClient

    def setup() {
        httpClient = Mock(HttpClient)
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
        origoClient.authorizeRequests "this-is-the-original-access-token"

    }

    def "Should be initialized"() {
        expect:
        origoClient != null
    }

    def "Should fail to authenticate with bad request"() {

        origoClient.metaClass.requestAccessToken = { -> new ResponseWrapper(400) }

        expect:
        !origoClient.authenticate()
        origoClient.accessToken == "this-is-the-original-access-token"

        cleanup:
        origoClient.metaClass = null
    }

    def "Should authenticate with good request"() {
        Object responseBody = '{"access_token" : "this-is-your-mock-access-token"}'

        origoClient.metaClass.requestAccessToken = { -> new ResponseWrapper(200, responseBody) }

        expect:
        origoClient.authenticate()
        origoClient.accessToken == "this-is-your-mock-access-token"

        cleanup:
        origoClient.metaClass = null
    }

    def "uploadUserPhoto should upload user photo."() {

        Object responseBody = '{"id" : "new-photo-id"}'

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[] {1})

        httpClient.makeRequest(_, _, _, _, _) >> new ResponseWrapper(200, responseBody)

        when:
        ResponseWrapper response = origoClient.uploadUserPhoto(photo, "jpg")

        then:
        response.success
        response.status == 200
        response.body.id == "new-photo-id"
    }

    def "accountPhotoApprove should approve user photo in mock API"() {

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[] {1})

        httpClient.makeRequest(_, _, _, _) >> new ResponseWrapper(200)

        when:
        ResponseWrapper response = origoClient.accountPhotoApprove(photo.person.identifier, "1")

        then:
        response.success
        response.status == 200
    }

}