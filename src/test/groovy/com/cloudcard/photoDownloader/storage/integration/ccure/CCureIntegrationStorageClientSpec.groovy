package com.cloudcard.photoDownloader.storage.integration.ccure

import com.cloudcard.photoDownloader.*
import spock.lang.Specification

class CCureIntegrationStorageClientSpec extends Specification {

    CCureIntegrationStorageClient integrationClient
    CCureClient cCureClient
    CloudCardClient cloudCardClient
    RestService restService
    LastRunPropertyService lastRunPropertyService
    
    static final String encodedBytes = 'Ynl0ZXM=' // the encoded version of "bytes"

    void setup() {
        integrationClient = new CCureIntegrationStorageClient()
        integrationClient.cCureClient = Mock(CCureClient)
        integrationClient.cloudCardClient = Mock(CloudCardClient)
        integrationClient.restService = Mock(RestService)
        integrationClient.lastRunPropertyService = Mock(LastRunPropertyService)
        integrationClient.createCCurePersonnel = false
        integrationClient.createRemotePhotoPerson = true
        integrationClient.employeeIdField = "Text1"

        cCureClient = integrationClient.cCureClient
        cloudCardClient = integrationClient.cloudCardClient
        restService = integrationClient.restService
        lastRunPropertyService = integrationClient.lastRunPropertyService
    }

    def "test pushPhoto with existing CloudCard person and approved photo"() {
        given:
        CCurePersonnel personnel = new CCurePersonnel(id: 123L, emailAddress: "test@example.com", partitionId: 1)
        Photo photo = new Photo(id: 1, status: "APPROVED", bytes: "bytes")
        Person person = new Person(
            email: "test@example.com",
            additionalProperties: [currentPhoto: photo]
        )
        photo.person = person

        when:
        integrationClient.pushPhoto(personnel)

        then:
        1 * cloudCardClient.findPersonByEmail("test@example.com") >> person
        1 * restService.fetchBytes(photo)
        1 * cCureClient.getPersonnelDetails(null, person.email) >> personnel
        1 * cCureClient.storePhoto(personnel.id, encodedBytes, 1, true)
        1 * lastRunPropertyService.updateLastRunTimestamp(_)
    }

    def "test pushPhoto with non-approved photo does not push to CCURE"() {
        given:
        CCurePersonnel personnel = new CCurePersonnel(id: 123L, emailAddress: "test@example.com", partitionId: 1)
        Photo photo = new Photo(id: 1, status: "PENDING")
        Person person = new Person(
            email: "test@example.com",
            additionalProperties: [currentPhoto: photo]
        )

        when:
        integrationClient.pushPhoto(personnel)

        then:
        1 * cloudCardClient.findPersonByEmail("test@example.com") >> person
        0 * restService.fetchBytes(_)
        0 * cCureClient.storePhoto(_, _, _, _)
        1 * lastRunPropertyService.updateLastRunTimestamp(_)
    }

    def "test pushPhoto creates CloudCard person when none exists"() {
        given:
        CCurePersonnel personnel = new CCurePersonnel(id: 123L, emailAddress: "test@example.com", partitionId: 1, employeeId: "emp123")

        when:
        integrationClient.pushPhoto(personnel)

        then:
        1 * cloudCardClient.findPersonByEmail("test@example.com") >> null
        1 * cloudCardClient.createPerson(personnel.emailAddress, personnel.employeeId)
        1 * lastRunPropertyService.updateLastRunTimestamp(_)
    }

    def "test pushPhoto ignores record when CloudCard person has no photo"() {
        given:
        CCurePersonnel personnel = new CCurePersonnel(id: 123L, emailAddress: "test@example.com", partitionId: 1)
        Person person = new Person(email: "test@example.com", additionalProperties: [:])

        when:
        integrationClient.pushPhoto(personnel)

        then:
        1 * cloudCardClient.findPersonByEmail(person.email) >> person
        0 * cloudCardClient.createPerson(_)
        1 * lastRunPropertyService.updateLastRunTimestamp(_)
    }

    def "test putPhoto finds CCURE personnel and stores photo"() {
        given:
        String identifier = "emp123"
        Person person = new Person(email: "test@example.com", customFields: [firstName: "John", lastName: "Doe"])
        Photo photo = new Photo(id: 1, person: person, bytes: "bytes")
        CCurePersonnel personnel = new CCurePersonnel(id: 456L, emailAddress: "test@example.com", partitionId: 1)

        when:
        integrationClient.putPhoto(identifier, photo)

        then:
        1 * cCureClient.getPersonnelDetails(identifier, person.email) >> personnel
        1 * cCureClient.storePhoto(456L, encodedBytes, 1, true)
    }

    def "test putPhoto throws exception when CCURE personnel not found and createCCurePersonnel is false"() {
        given:
        String identifier = "emp123"
        Person person = new Person(email: "test@example.com")
        Photo photo = new Photo(id: 1, person: person, bytes: "bytes")
        integrationClient.createCCurePersonnel = false

        when:
        integrationClient.putPhoto(identifier, photo)

        then:
        1 * cCureClient.getPersonnelDetails(identifier, person.email) >> null
        0 * cCureClient.createPersonnel(_, _, _, _)
        thrown(FailedPhotoFileException)
    }

    def "test putPhoto creates CCURE personnel when not found and createCCurePersonnel is true"() {
        given:
        String identifier = "emp123"
        Person person = new Person(email: "test@example.com", customFields: [firstName: "John", lastName: "Doe"])
        Photo photo = new Photo(id: 1, person: person, bytes: "bytes")
        integrationClient.createCCurePersonnel = true
        integrationClient.firstNameField = "firstName"
        integrationClient.lastNameField = "lastName"

        when:
        integrationClient.putPhoto(identifier, photo)

        then:
        1 * cCureClient.getPersonnelDetails(identifier, person.email) >> null
        1 * cCureClient.createPersonnel("John", "Doe", person.email, identifier) >> 789L
        1 * cCureClient.getPersonnelDetails(identifier, person.email) >> new CCurePersonnel(id: 789L, emailAddress: "test@example.com", partitionId: 1)
        1 * cCureClient.storePhoto(789L, encodedBytes, 1, true)
    }

    def "test putPhoto throws exception when CCURE personnel creation fails"() {
        given:
        String identifier = "emp123"
        Person person = new Person(email: "test@example.com", customFields: [firstName: "John", lastName: "Doe"])
        Photo photo = new Photo(id: 1, person: person, bytes: "bytes")
        integrationClient.createCCurePersonnel = true
        integrationClient.firstNameField = "firstName"
        integrationClient.lastNameField = "lastName"

        when:
        integrationClient.putPhoto(identifier, photo)

        then:
        1 * cCureClient.getPersonnelDetails(identifier, person.email) >> null
        1 * cCureClient.createPersonnel("John", "Doe", person.email, identifier) >> null
        0 * cCureClient.storePhoto(_, _, _, _)
        thrown(FailedPhotoFileException)
    }

    def "test putPhoto rethrows exception when CCURE personnel creation fails"() {
        given:
        String identifier = "emp123"
        Person person = new Person(email: "test@example.com", customFields: new HashMap<String, Object>())
        Photo photo = new Photo(id: 1, person: person, bytes: "bytes")
        integrationClient.createCCurePersonnel = true

        when:
        integrationClient.putPhoto(identifier, photo)

        then:
        1 * cCureClient.getPersonnelDetailsByEmail("test@example.com") >> null
        1 * cCureClient.createPersonnel(null, null, "test@example.com") >> { throw new FailedPhotoFileException("Name fields are missing")}
        0 * cCureClient.storePhoto(_, _)
        thrown(FailedPhotoFileException)
    }

    def "test putPhoto fails when CCURE personnel ID cannot be found"() {
        given:
        String identifier = "emp123"
        Person person = new Person(email: "test@example.com")
        Photo photo = new Photo(id: 1, person: person, bytes: "bytes")
        CCurePersonnel personnel = new CCurePersonnel(id: null, emailAddress: "test@example.com", partitionId: 1)
        integrationClient.createCCurePersonnel = false

        when:
        integrationClient.putPhoto(identifier, photo)

        then:
        1 * cCureClient.getPersonnelDetails(identifier, person.email) >> personnel
        0 * cCureClient.storePhoto(_, _, _, _)
        thrown(FailedPhotoFileException)
    }

    def "test putPhoto swallows generic exceptions"() {
        given:
        String identifier = "emp123"
        Person person = new Person(email: "test@example.com")
        Photo photo = new Photo(id: 1, person: person, bytes: "bytes")

        when:
        integrationClient.putPhoto(identifier, photo)

        then:
        1 * cCureClient.getPersonnelDetails(identifier, person.email) >> { throw new RuntimeException("Network error") }
    }

    def "test putPhoto rethrows FailedPhotoFileException without wrapping"() {
        given:
        String identifier = "emp123"
        Person person = new Person(email: "test@example.com")
        Photo photo = new Photo(id: 1, person: person, bytes: "bytes")

        when:
        integrationClient.putPhoto(identifier, photo)

        then:
        1 * cCureClient.getPersonnelDetails(identifier, person.email) >> { throw new FailedPhotoFileException("Already a FailedPhotoFileException") }
        def exception = thrown(FailedPhotoFileException)
        exception.message == "Already a FailedPhotoFileException"
    }

    def "test close does nothing"() {
        when:
        integrationClient.close()

        then:
        noExceptionThrown()
    }

    def "test init authenticates and processes audit logs"() {
        given:
        CCurePersonnel personnel1 = new CCurePersonnel(id: 1L, emailAddress: "user1@example.com", partitionId: 1, employeeId: "emp123")
        CCurePersonnel personnel2 = new CCurePersonnel(id: 2L, emailAddress: "user2@example.com", partitionId: 1, employeeId: "emp456")
        List<CCurePersonnel> auditLogs = [personnel1, personnel2]

        when:
        integrationClient.init()

        then:
        1 * cCureClient.authenticate()
        1 * cCureClient.queryAuditLogsForNewPeople() >> auditLogs
        2 * cloudCardClient.findPersonByEmail(_) >> null
        2 * cloudCardClient.createPerson(_, _)
        1 * cCureClient.subscribeForNewEvents(_)
        2 * lastRunPropertyService.updateLastRunTimestamp(_)
    }

    def "test init subscribes to new events and processes ObjectCreated notifications"() {
        given:
        def capturedCallback

        when:
        integrationClient.init()

        then:
        1 * cCureClient.authenticate()
        1 * cCureClient.queryAuditLogsForNewPeople() >> []
        1 * cCureClient.subscribeForNewEvents(_) >> { args ->
            capturedCallback = args[0]
        }

        when:
        def payload = [
            NotificationType: "ObjectCreated",
            NotificationDSO: [
                ObjectID: 999L,
                EmailAddress: "newuser@example.com",
                "Text1": "emp123"
            ]
        ]
        capturedCallback(payload)

        then:
        1 * cloudCardClient.findPersonByEmail("newuser@example.com") >> null
        1 * cloudCardClient.createPerson("newuser@example.com", "emp123")
        1 * lastRunPropertyService.updateLastRunTimestamp(_)
    }

    def "test init ignores non-ObjectCreated notifications"() {
        given:
        def capturedCallback

        when:
        integrationClient.init()

        then:
        1 * cCureClient.authenticate()
        1 * cCureClient.queryAuditLogsForNewPeople() >> []
        1 * cCureClient.subscribeForNewEvents(_) >> { args ->
            capturedCallback = args[0]
        }

        when:
        def payload = [
            NotificationType: "ObjectModified",
            NotificationDSO: [
                ObjectID: 999L,
                EmailAddress: "user@example.com",
                Text1: "emp123"
            ]
        ]
        capturedCallback(payload)

        then:
        0 * cloudCardClient.findPersonByEmail(_)
        0 * cloudCardClient.createPerson(_, _)
        0 * lastRunPropertyService.updateLastRunTimestamp(_)
    }
}
