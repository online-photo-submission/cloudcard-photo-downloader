package com.cloudcard.photoDownloader

import software.amazon.awssdk.services.sqs.model.Message
import spock.lang.Specification

class PhotoSpec extends Specification {

    final static int photoId = 1425800
    final static String messageBody = """
{
    "person": {
        "id": 618543,
        "username": "jonathan+testThisJawn@cloudcard.us",
        "email": "jonathan+testThisJawn@cloudcard.us",
        "identifier": "123456789",
        "customFields": {
            "Full Name": "Frodo Baggins",
            "Last Name": "Baggins",
            "Site": "asdfasdf",
            "Card Pickup Location": "Mailing",
            "Start Date": null,
            "First Name": "Frodo"
        },
        "organization": {
            "id": 111,
            "name": "Jonathan's Test Org",
            "identifier": null
        },
        "additionalPhotos": [
            {
                "id": 76309,
                "additionalPhotoType": "Government Issued ID",
                "publicKey": "additional-photo-100348.2024.240.yg44Lif0GR6S1ck1mNTGnXYP4RPpmK4rdxgHeBSpkNIk4uDgrrMPV7tJOf0IgW4B",
                "links": {
                    "bytes": "https://s3.amazonaws.com/test-photos.onlinephotosubmission.com/additional-photo-100348.2024.240.yg44Lif0GR6S1ck1mNTGnXYP4RPpmK4rdxgHeBSpkNIk4uDgrrMPV7tJOf0IgW4B.jpg"
                },
                "facialRecognitionScore": null,
                "govIdScore": 0.841179,
                "dateCreated": "2024-08-27T18:19:59+0000",
                "lastUpdated": "2024-08-27T18:20:00+0000",
                "width": 1076,
                "height": 1578,
                "expirationDate": "2025-02-23T18:19:59+0000"
            }
        ],
        "additionalPhotoRequired": true
    },
    "helperBotAction": null,
    "dateCreated": "2024-08-27T18:19:54+0000",
    "id": $photoId,
    "links": {
        "bytes": "https://s3.amazonaws.com/test-photos.onlinephotosubmission.com/100348.24.240.m3uV8hi5s0W9fO5ZYyLk2KpgGzgwZNj9w61IVVMh2KpEVv8ZBrjJzOG21f03Czt8ujcKY0Olxn2jfQ59.jpg"
    },
    "lowestClassification": null,
    "backgroundReplaced": true,
    "helperBotReviewed": true,
    "isAspectRatioCorrect": true,
    "domainClass": "us.cloudcard.api.Photo",
    "publicKey": "100348.24.240.m3uV8hi5s0W9fO5ZYyLk2KpgGzgwZNj9w61IVVMh2KpEVv8ZBrjJzOG21f03Czt8ujcKY0Olxn2jfQ59",
    "manuallyEdited": false,
    "status": "DOWNLOADED",
    "originalPhoto": {
        "id": 1425798,
        "publicKey": "100348.24.240.K3cn1GJ1cW4GltVNuIWyzn3Cmon4mgyIPlfYb032TXAKXtO45s28mQjKqXJsIclPKgn8lJ7HsXCE3mi4",
        "links": {
            "bytes": "https://s3.amazonaws.com/test-photos.onlinephotosubmission.com/100348.24.240.K3cn1GJ1cW4GltVNuIWyzn3Cmon4mgyIPlfYb032TXAKXtO45s28mQjKqXJsIclPKgn8lJ7HsXCE3mi4.jpg"
        }
    },
    "version": 4,
    "aspectRatio": 0.9950248756218906
}
"""

//    def setup() {
//        println "setting up"
//        String bacon = "bacon bacon"
//    }
//
    def "test from JSON"() {
        given:
        Message message = GroovyMock(Message) {
            body() >> messageBody
        }
        assert message.body() == messageBody

        when:
        Photo result = Photo.fromSqsMessage(message)

        then:
        result?.id == photoId
        result.status == "DOWNLOADED"

        result.publicKey == "100348.24.240.m3uV8hi5s0W9fO5ZYyLk2KpgGzgwZNj9w61IVVMh2KpEVv8ZBrjJzOG21f03Czt8ujcKY0Olxn2jfQ59"
        result.links.bytes == "https://s3.amazonaws.com/test-photos.onlinephotosubmission.com/100348.24.240.m3uV8hi5s0W9fO5ZYyLk2KpgGzgwZNj9w61IVVMh2KpEVv8ZBrjJzOG21f03Czt8ujcKY0Olxn2jfQ59.jpg"
        result.externalURL == "https://s3.amazonaws.com/test-photos.onlinephotosubmission.com/100348.24.240.m3uV8hi5s0W9fO5ZYyLk2KpgGzgwZNj9w61IVVMh2KpEVv8ZBrjJzOG21f03Czt8ujcKY0Olxn2jfQ59.jpg"

        result.person.id == 618543
        result.person.username == "jonathan+testThisJawn@cloudcard.us"
        result.person.email == "jonathan+testThisJawn@cloudcard.us"
        result.person.identifier == "123456789"

        !result.manuallyEdited
        result.dateCreated == new Date(2024-1900, Calendar.AUGUST, 27, 14, 19, 54)
        result.version == 4
        result.backgroundReplaced
        result.helperBotReviewed
        result.aspectRatio == 0.9950248756218906
        result.lowestClassification == null
    }
}
