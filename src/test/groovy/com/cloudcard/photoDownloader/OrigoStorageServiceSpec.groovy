package com.cloudcard.photoDownloader

import spock.lang.Specification
import spock.lang.Unroll

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
        origoClient.authorizeRequests "this-is-the-original-access-token"

    }

    def "save (multiple) should return empty when passed empty list"() {
        List<PhotoFile> photoFiles = origoStorageService.save([])

        expect:
        !photoFiles[0]
    }

    def "should authenticate if client is logged out"() {
        origoStorageService.save([])

        OrigoClient _origoClient = Mock()
        _origoClient.isAuthenticated >> false
        origoStorageService.origoClient = _origoClient

        when:
        origoStorageService.save([new Photo()])

        then:
        1 * origoStorageService.origoClient.authenticate()
    }

    def "should iterate through and save a list of photos"() {
        List<Photo> photos = [
                new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[]{1}),
                new Photo(id: 2, person: new Person(identifier: "person-2"), bytes: new byte[]{1}),
                new Photo(id: 3, person: new Person(identifier: "person-3"), bytes: new byte[]{1}),
                new Photo(id: 4, person: new Person(identifier: "person-4"), bytes: new byte[]{1}),
                new Photo(id: 5, person: new Person(identifier: "person-5"), bytes: new byte[]{1}),
                new Photo(id: 6, person: new Person(identifier: "person-6"), bytes: new byte[]{1}),
                new Photo(id: 7, person: new Person(identifier: "person-7"), bytes: new byte[]{1}),
                new Photo(id: 8, person: new Person(identifier: "person-8"), bytes: new byte[]{1}),
                new Photo(id: 9, person: new Person(identifier: "person-9"), bytes: new byte[]{1}),
                new Photo(id: 10, person: new Person(identifier: "person-10"), bytes: new byte[]{1})
        ]

        Object uploadResponse = '{"id" : "new-photo-id"}'

        OrigoClient _origoClient = Mock()
        _origoClient.isAuthenticated >> true
        _origoClient.uploadUserPhoto(_, _) >> new ResponseWrapper(200, uploadResponse)
        _origoClient.accountPhotoApprove(_, _) >> new ResponseWrapper(200)

        origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> "png"
        }

        origoStorageService.origoClient = _origoClient

        when:
        List<PhotoFile> files = origoStorageService.save(photos)

        then:
        files.size() == 10
        10 * origoStorageService.save(_ as Photo)
    }

    def "should save 8 of 10 photos"() {
        List<Photo> photos = [
                new Photo(id: 1, person: new Person(identifier: "person-1"), bytes: new byte[]{1}),
                new Photo(id: 2, person: new Person(identifier: "person-2"), bytes: new byte[]{1}),
                new Photo(id: 3, person: new Person(identifier: "person-3"), bytes: new byte[]{1}),
                new Photo(id: 4, person: new Person(identifier: "person-4"), bytes: new byte[]{1}),
                new Photo(id: 5, person: null, bytes: new byte[]{1}),
                new Photo(id: 6, person: new Person(identifier: "person-6"), bytes: new byte[]{1}),
                new Photo(id: 7, person: null, bytes: new byte[]{1}),
                new Photo(id: 8, person: new Person(identifier: "person-8"), bytes: new byte[]{1}),
                new Photo(id: 9, person: new Person(identifier: "person-9"), bytes: new byte[]{1}),
                new Photo(id: 10, person: new Person(identifier: "person-10"), bytes: new byte[]{1})
        ]

        Object uploadResponse = '{"id" : "new-photo-id"}'

        OrigoClient _origoClient = Mock()
        _origoClient.isAuthenticated >> true
        _origoClient.uploadUserPhoto(_, _) >> new ResponseWrapper(200, uploadResponse)
        _origoClient.accountPhotoApprove(_, _) >> new ResponseWrapper(200)

        origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> "png"
        }

        origoStorageService.origoClient = _origoClient

        when:
        List<PhotoFile> files = origoStorageService.save(photos)

        then:
        files.size() == 8
        10 * origoStorageService.save(_ as Photo)
    }

    def "should not save a photo with a null person property"() {
        Photo photo = new Photo(id: 5, person: null, bytes: new byte[]{1})

        expect:
        !origoStorageService.save(photo)
    }

    @Unroll
    def "should not save photo with incorrect #description filetype"() {
        origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> extension
        }

        boolean correctExtension = ["jpg", "png"].contains(extension)

        when:
        origoStorageService.save(photo)

        then:
        correctExtension && photoFile
        !correctExtension && !photoFile

        where:
        extension | description | photo                                                                            || photoFile
        ""        | "blank"     | new Photo(id: 1, person: new Person(identifier: "person"), bytes: new byte[]{1}) || null
        "jpeg"        | "jpeg"     | new Photo(id: 1, person: new Person(identifier: "person"), bytes: new byte[]{1}) || null
        "pdf"        | "pdf"     | new Photo(id: 1, person: new Person(identifier: "person"), bytes: new byte[]{1}) || null
        "docx"        | "docx"     | new Photo(id: 1, person: new Person(identifier: "person"), bytes: new byte[]{1}) || null
        "docx"        | "docx"     | new Photo(id: 1, person: new Person(identifier: "person"), bytes: new byte[]{1}) || null
    }
}
