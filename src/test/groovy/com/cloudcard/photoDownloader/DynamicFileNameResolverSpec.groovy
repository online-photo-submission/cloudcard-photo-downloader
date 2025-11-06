package com.cloudcard.photoDownloader

import spock.lang.Specification

class DynamicFileNameResolverSpec extends Specification {

    DynamicFileNameResolver resolver = new DynamicFileNameResolver()
    Photo photo

    def setup() {
        resolver.delimiter = "_"
        resolver.include = ["John", "Mary", "Bob", "identifier"] as String[]
        resolver.dateFormat = "yyyy-MM-dd_HH-mm-ss"
        photo = new Photo()
        photo.setPerson(new Person())
        photo.getPerson().setIdentifier("100")
        photo.getPerson().setCustomFields(new HashMap<>())
        photo.getPerson().setEmail("foo@bar.edu")
        photo.dateCreated = new Date(2025-1900, 0, 22, 13, 15, 7)
        Map customFields = photo.getPerson().getCustomFields()
        customFields.put("John", "bacon")
        customFields.put("Mary", "bacon1")
        customFields.put("Bob", "bacon2")
    }

    def "test with 3 custom fields"() {
        expect:
        resolver.getBaseName(photo) == "bacon_bacon1_bacon2_100"
    }

    def "test with 2 custom fields"() {
        given:
        resolver.include = ["John", "Mary", "identifier"] as String[]

        expect:
        resolver.getBaseName(photo) == "bacon_bacon1_100"
    }

    def "test with 2 custom fields identifier and date created"() {
        given:
        resolver.include = ["John", "Mary", "identifier", "dateCreated"] as String[]

        expect:
        resolver.getBaseName(photo) == "bacon_bacon1_100_2025-01-22_13-15-07"
    }

    def "test with 1 custom field and no identifier"() {
        given:
        resolver.include = ["John"] as String[]

        expect:
        resolver.getBaseName(photo) == "bacon"
    }

    def "test delimiter"() {
        given:
        resolver.delimiter = "."

        expect:
        resolver.getBaseName(photo) == "bacon.bacon1.bacon2.100"
    }

    def "test missing custom fields"() {
        given:
        Map customFields = photo.getPerson().getCustomFields()
        customFields.put("John", "")
        customFields.put("Mary", "")
        customFields.put("Bob", "")

        expect:
        resolver.getBaseName(photo) == null
    }

    def "test null custom fields"() {
        given:
        photo.getPerson().setCustomFields(new HashMap<>())

        expect:
        resolver.getBaseName(photo) == null
    }
}
