package com.cloudcard.photoDownloader

import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonSlurper

class PersonSpec extends spock.lang.Specification {

//    JsonSlurper slurper = new JsonSlurper()
    ObjectMapper mapper = new ObjectMapper()
    // get a sample object, test the json conversion on different type of custom fields

    def "test customFields from a Person api response"() {
        given:
        def json = """
{
"id" : 632332,
"customFields" : {
    "Floor" : {
      "id" : 173,
      "customField" : {
        "id" : 7
      },
      "lastUpdated" : "2026-03-11T19:25:40Z",
      "person" : {
        "id" : 632332
      },
      "value" : "1",
      "ocrClassification" : "unclassified"
    }
  }
}
"""
        when:
        Person person = mapper.readValue(json, Person.class)

        then:
        person?.id == 632332
        person?.customFields?.size() == 1
        person?.customFields?.containsKey("Floor")
        person?.customFields?.get("Floor") == "1"
    }

    def "test customFields from a downloaded Photo message"() {
        given:
        def json = """
{
"id" : 632332,
"customFields" : {
    "Floor" : "1"
  }
}
"""
        when:
        Person person = mapper.readValue(json, Person.class)

        then:
        person?.id == 632332
        person?.customFields?.size() == 1
        person?.customFields?.containsKey("Floor")
        person?.customFields?.get("Floor") == "1"
    }

    def "test customFields a numeric value"() {
        given:
        def json = """
{
"id" : 632332,
"customFields" : {
    "Floor" : 1
  }
}
"""
        when:
        Person person = mapper.readValue(json, Person.class)

        then:
        person?.id == 632332
        person?.customFields?.size() == 1
        person?.customFields?.containsKey("Floor")
        person?.customFields?.get("Floor") == "1"
    }

    def "test additionalPhotos from a Person API response is discarded"() {
        given:
        def json = """
{
"id" : 632332,
"additionalPhotos" : {
    "Government ID" : {
      "id" : 181,
      "width" : 721,
      "externalURL" : null
    }
  }
}
"""
        when:
        Person person = mapper.readValue(json, Person.class)

        then:
        person?.id == 632332
        person?.additionalPhotos?.isEmpty()
    }

    def "test additionalPhotos from a downloaded Photo message"() {
        given:
        def json = """
{
"id" : 632332,
"additionalPhotos" : [
    {
        "id": 75755,
        "additionalPhotoType": {
            "name": "Government issued ID",
            "enableOcr": false,
            "enableFacialRecognition": false,
            "enableGovIdClassifier": false,
            "goodFacialRecognitionThreshold": 0.9,
            "badFacialRecognitionThreshold": 0.5
        },
        "facialRecognitionScore": null,
        "facialRecognitionClassification": "unclassified",
        "govIdScore": null,
        "govIdClassification": "unclassified",
        "lowestClassification": "unclassified",
        "publicKey": "additional-photo-8.2022.237.rhtq56r50kimup2eeocqwocicmkeefdjf574oefip5i2l6k2nvqongb2c4kgnek1l",
        "links": {
            "bytes": "https://s3.amazonaws.com/test-photos.onlinephotosubmission.com/additional-photo-8.2022.237.rhtq56r50kimup2eeocqwocicmkeefdjf574oefip5i2l6k2nvqongb2c4kgnek1l.jpg"
        }
    }
]
}
"""
        when:
        Person person = mapper.readValue(json, Person.class)

        then:
        person?.id == 632332
        person?.additionalPhotos?.size() == 1
        person?.additionalPhotos?.get(0)?.typeName == "Government issued ID"
    }
}
