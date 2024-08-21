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
    def "should save photos with correct filetype: #description"() {
        origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> extension
        }

        Photo photo = new Photo(id: testNumber, person: new Person(identifier: "person-$testNumber", email: "hello$testNumber@mail.com"), bytes: new byte[]{1})
        Object uploadResponse = '{"id" : "new-photo-id"}'

        OrigoClient _origoClient = Mock()
        _origoClient.isAuthenticated >> true
        _origoClient.uploadUserPhoto(_, _) >> new ResponseWrapper(200, uploadResponse)
        _origoClient.accountPhotoApprove(_, _) >> new ResponseWrapper(200)
        origoStorageService.origoClient = _origoClient

        when:
        PhotoFile result = origoStorageService.save(photo)

        then:

        (result == null) == photoFileIsNull

        where:
        extension | description                  | testNumber || photoFileIsNull
        ""        | "\"\"     - failed to save." | 1          || true
        "jpeg"    | "\"jpeg\" - failed to save." | 2          || true
        "jpg"     | "\"jpg\"  - saved."          | 3          || false
        "pdf"     | "\"pdf\"  - failed to save." | 4          || true
        "png"     | "\"png\"  - saved."          | 5          || false
        "docx"    | "\"docx\" - failed to save." | 6          || true
    }

    def "should not save a photo if upload fails"() {
        def origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> "jpg"
        }

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), bytes: new byte[]{1})

        OrigoClient _origoClient = Mock()
        _origoClient.uploadUserPhoto(_, _) >> new ResponseWrapper(400, "Bad Request")
        _origoClient.authenticate() >> true
        origoStorageService.origoClient = _origoClient

        when:
        PhotoFile photoFile = origoStorageService.save(photo)

        then:
        photoFile == null

    }

    def "should approve photo if upload is successful"() {
        Object uploadResponse = '{"id" : "new-photo-id"}'

        def origoStorageService = Spy(OrigoStorageService) {
            resolveAccountId(_) >> "123"
            resolveFileType(_) >> "jpg"
        }

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), bytes: new byte[]{1})

        OrigoClient _origoClient = Mock()
        _origoClient.uploadUserPhoto(_, _) >> new ResponseWrapper(200, uploadResponse)
        _origoClient.authenticate() >> true
        origoStorageService.origoClient = _origoClient

        when:
        PhotoFile photoFile = origoStorageService.save(photo)

        then:
        1 * origoStorageService.origoClient.accountPhotoApprove(_, _) >> new ResponseWrapper(200)
        photoFile != null
    }

    @Unroll
    def "resolveFileType should pull file type from aws png or jpg link - #description "() {

        Photo photo = new Photo(id: 1, person: new Person(identifier: "person-1", email: "hello1@mail.com"), links: links, bytes: new byte[]{1})

        when:
        String fileType = origoStorageService.resolveFileType(photo)

        then:
        fileType == result

        where:
        links                                                      | description         || result
        new Links(bytes: "https://api.aws.fake.com/some-file.jpg") | "\"jpg\" - Correct" || "jpg"
        new Links(bytes: "https://api.aws.fake.com/some-file.png") | "\"png\" - Correct" || "png"
        new Links(bytes: "https://api.aws.fake.com/some-file.exe") | "\"\" - Incorrect"  || ""
    }

    @Unroll
    def "resolveFileType should return empty string with invalid aws link - result #description"() {
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

}
