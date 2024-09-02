package com.cloudcard.photoDownloader

import spock.lang.Specification
import spock.lang.Unroll

class OrigoStorageServiceSpec extends Specification {
    OrigoStorageService origoStorageService

    OrigoClient origoClient

    def setup() {
        origoStorageService = new OrigoStorageService()

        FileNameResolver fileNameResolver = Mock()
        origoStorageService.fileNameResolver = fileNameResolver

        origoClient = Mock(OrigoClient)
        SimpleResponseLogger simpleResponseLogger = Mock()
        origoClient.simpleResponseLogger = simpleResponseLogger

        origoClient.eventManagementApi = "https://api.mock-event-management.com"
        origoClient.mobileIdentitiesApi = "https://api.mock-mobile-identities.com"
        origoClient.certIdpApi = "https://api.mock-cert-idp.com"
        origoClient.organizationId = "12345"
        origoClient.contentType = "application/vnd.assaabloy.ma.credential-management-2.2+json"
        origoClient.applicationVersion = "2.2"
        origoClient.applicationId = "ORIGO-APPLICATION"
        origoClient.clientId = "67890"
        origoClient.clientSecret = "Password1234"

        origoStorageService.origoClient = origoClient
    }

    def "should be initialized"() {
        expect:
        origoStorageService != null
    }

    def "origoClient should be initialized"() {
        expect:
        origoStorageService.origoClient != null
    }

    def "filenameResolver should be initialized"() {
        expect:
        origoStorageService.fileNameResolver != null
    }

    def "save (multiple) should return empty when passed empty list"() {
        given:
        List<PhotoFile> photoFiles = origoStorageService.save([])

        expect:
        !photoFiles[0]
    }

    def "save (multiple) should iterate through and save a list of photos"() {
        given:
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
        origoStorageService = Spy(OrigoStorageService)

        when:
        List<PhotoFile> files = origoStorageService.save(photos)

        then:
        files.size() == 10
        10 * origoStorageService.save(_ as Photo) >> new PhotoFile("baseName", null, 123)
    }

    def "should not save a photo with a null person property"() {
        given:
        Photo photo = new Photo(id: 5, person: null, bytes: new byte[]{1})

        expect:
        !origoStorageService.save(photo)
    }

    @Unroll
    def "should save photos with correct filetype: #description"() {
        given:
        origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> extension
        }

        Photo photo = new Photo(id: testNumber, person: new Person(identifier: "person-$testNumber", email: "hello$testNumber@mail.com"), bytes: new byte[]{1})

        origoClient.makeAuthenticatedRequest(_) >> new ResponseWrapper(200)

        origoStorageService.origoClient = origoClient

        when:
        PhotoFile photoFile = origoStorageService.save(photo)

        then:
        photoFile == result

        where:
        extension | description                  | testNumber || result
        ""        | "\"\"     - failed to save." | 1          || null
        "jpeg"    | "\"jpeg\" - failed to save." | 2          || null
        "jpg"     | "\"jpg\"  - saved."          | 3          || new PhotoFile("123", null, 3)
        "pdf"     | "\"pdf\"  - failed to save." | 4          || null
        "png"     | "\"png\"  - saved."          | 5          || new PhotoFile("123", null, 5)
        "docx"    | "\"docx\" - failed to save." | 6          || null
    }

    def "should not save a photo if upload fails and replaceExistingPhotos is false"() {
        given:
        def origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> "jpg"
            it.replaceExistingPhotos >> false
        }

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), bytes: new byte[]{1})

        origoClient.makeAuthenticatedRequest(_) >> new ResponseWrapper(400)

        origoStorageService.origoClient = origoClient

        when:
        PhotoFile photoFile = origoStorageService.save(photo)

        then:
        photoFile == null
    }

    def "should removeCurrentPhoto and save if upload fails but replaceExistingPhotos is true"() {
        given:
        def origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> "jpg"
            it.replaceExistingPhotos = true
        }

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), bytes: new byte[]{1})

        origoStorageService.origoClient = origoClient

        when:
        PhotoFile photoFile = origoStorageService.save(photo)

        then:
        1 * origoStorageService.removeCurrentPhoto(_) >> {}
        3 * origoClient.makeAuthenticatedRequest(_) >> new ResponseWrapper(400) >> new ResponseWrapper(200) >> new ResponseWrapper(200)
        photoFile != null
    }

    def "should return null if second upload attempt fails"() {
        given:
        def origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> "jpg"
            it.replaceExistingPhotos = true
        }

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), bytes: new byte[]{1})

        origoStorageService.origoClient = origoClient

        when:
        PhotoFile photoFile = origoStorageService.save(photo)

        then:
        1 * origoStorageService.removeCurrentPhoto(_) >> {}
        2 * origoClient.makeAuthenticatedRequest(_) >> new ResponseWrapper(400) >> new ResponseWrapper(400)
        photoFile == null
    }

    def "should return photofile if photo approval succeeds"() {
        given:
        def origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> "jpg"
            it.replaceExistingPhotos = true
        }

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), bytes: new byte[]{1})

        origoStorageService.origoClient = origoClient

        when:
        PhotoFile photoFile = origoStorageService.save(photo)

        then:
        1 * origoStorageService.removeCurrentPhoto(_) >> {}
        3 * origoClient.makeAuthenticatedRequest(_) >> new ResponseWrapper(400) >> new ResponseWrapper(200) >> new ResponseWrapper(200)
        photoFile != null
    }

    def "should return null if photo approval fails"() {
        given:
        def origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> "jpg"
            it.replaceExistingPhotos = true
        }

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), bytes: new byte[]{1})

        origoStorageService.origoClient = origoClient

        when:
        PhotoFile photoFile = origoStorageService.save(photo)

        then:
        1 * origoStorageService.removeCurrentPhoto(_) >> {}
        3 * origoClient.makeAuthenticatedRequest(_) >> new ResponseWrapper(400) >> new ResponseWrapper(200) >> new ResponseWrapper(400)
        photoFile == null
    }

    @Unroll
    def "resolveFileType should pull file type from aws png or jpg link - #description "() {
        given:
        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), links: links, bytes: new byte[]{1})

        when:
        String fileType = origoStorageService.resolveFileType(photo)

        then:
        fileType == result

        where:
        links                                                      | description           || result
        new Links(bytes: "https://api.aws.fake.com/some-file.jpg") | "\"jpg\" - Correct"   || "jpg"
        new Links(bytes: "https://api.aws.fake.com/some-file.png") | "\"png\" - Correct"   || "png"
        new Links(bytes: "https://api.aws.fake.com/some-file.exe") | "\"exe\" - Incorrect" || "exe"
    }

    @Unroll
    def "resolveFileType should return empty string with invalid aws link - result #description"() {
        given:
        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), links: links, bytes: new byte[]{1})

        when:
        String fileType = origoStorageService.resolveFileType(photo)

        then:
        fileType == result

        where:
        links                  | description             || result
        null                   | "\"null\""              || ""
        new Links(bytes: "jp") | "\"Link is too short\"" || ""

    }

    def "removeCurrentPhoto should get user details and delete user photo"() {
        given:
        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), bytes: new byte[]{1})
        String userDetails = '{"urn:hid:scim:api:ma:2.2:User:Photo":{"id":[123456]}}'

        when:
        origoStorageService.removeCurrentPhoto(photo)

        then:
        2 * origoStorageService.origoClient.makeAuthenticatedRequest(_) >> new ResponseWrapper(200, userDetails) >> new ResponseWrapper(200)
    }

    def "removeCurrentPhoto shouldn't delete if getUserDetails fails"() {
        given:
        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), bytes: new byte[]{1})

        when:
        origoStorageService.removeCurrentPhoto(photo)

        then:
        1 * origoStorageService.origoClient.makeAuthenticatedRequest(_) >> new ResponseWrapper(400)
    }
}
