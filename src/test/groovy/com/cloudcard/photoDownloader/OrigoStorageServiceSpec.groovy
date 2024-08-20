package com.cloudcard.photoDownloader

import spock.lang.Specification

class OrigoStorageServiceSpec extends Specification {
    OrigoStorageService origoStorageService

    OrigoClient origoClient

    def setup() {
        origoStorageService = new OrigoStorageService()

        origoClient = new OrigoClient()
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

    def "save (multiple) should fail when passed empty list"() {
        List<PhotoFile> photoFiles = origoStorageService.save([])

        expect:
        !photoFiles[0]
    }

    def "should authenticate if necessary"() {
        List<PhotoFile> photoFiles = origoStorageService.save([])

        OrigoClient _origoClient = Mock()
        _origoClient.isAuthenticated = false
        origoStorageService.origoClient = _origoClient

        when:
        origoStorageService.save([new Photo()])

        then:
        1 * origoStorageService.origoClient.authenticate()
    }

//    def "should iterate through and save a list of photos"() {
//        List<Photo> photos = [
//                new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[] {1})
//        ]
//    }
}
