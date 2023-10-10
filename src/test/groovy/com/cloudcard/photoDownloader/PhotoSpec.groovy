package com.cloudcard.photoDownloader

import software.amazon.awssdk.services.sqs.model.Message
import spock.lang.Specification

class PhotoSpec extends Specification {

    final static int photoId = 3965
    final static String messageBody = """
{
    "domainClass": "us.cloudcard.api.Photo",
    "publicKey": "000002.23.236.icoAtl6vF16dhdvLD595GA91RhTco4e7Us4nuL22fA7o0069EqoP3i750e8QbIT3OqfVd28crlDEthuM",
    "manuallyEdited": true,
    "person": {
        "id": 944,
        "username": "tony+hg9@sharptop.co",
        "email": "tony+hg9@sharptop.co",
        "identifier": "770123",
        "customFields": {
            "Preferred Name": "fsdfsdk",
            "Legal Name": "",
            "Card Type": "Mobile sadsa",
            "Campus": "No Response"
        },
        "organization": {
            "id": 2,
            "name": "CloudCard",
            "identifier": "sharptop"
        },
        "additionalPhotos": [],
        "additionalPhotoRequired": true
    },
    "status": "DOWNLOADED",
    "originalPhoto": {
        "id": 3963,
        "publicKey": "000002.23.236.27Chs0aA36aPFKr0TRJ8c57S6M5gcRpktu6QtDe78i30581stVhNCo9NdF24oNpRk0alD617c4bq4p79",
        "links": {
            "bytes": "https://s3-ca-central-1.amazonaws.com/photos.cloudcard/000002.23.236.27Chs0aA36aPFKr0TRJ8c57S6M5gcRpktu6QtDe78i30581stVhNCo9NdF24oNpRk0alD617c4bq4p79.jpg"
        }
    },
    "dateCreated": "2023-08-24T20:28:28+0000",
    "id": $photoId,
    "links": {
        "bytes": "https://s3-ca-central-1.amazonaws.com/photos.cloudcard/000002.23.236.icoAtl6vF16dhdvLD595GA91RhTco4e7Us4nuL22fA7o0069EqoP3i750e8QbIT3OqfVd28crlDEthuM.jpg"
    },
    "version": 5,
    "lowestClassification": 0.4954,
    "backgroundReplaced": true,
    "aspectRatio": 1.0,
    "helperBotReviewed": true,
    "isAspectRatioCorrect": true
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

        result.publicKey == "000002.23.236.icoAtl6vF16dhdvLD595GA91RhTco4e7Us4nuL22fA7o0069EqoP3i750e8QbIT3OqfVd28crlDEthuM"
        result.links.bytes == "https://s3-ca-central-1.amazonaws.com/photos.cloudcard/000002.23.236.icoAtl6vF16dhdvLD595GA91RhTco4e7Us4nuL22fA7o0069EqoP3i750e8QbIT3OqfVd28crlDEthuM.jpg"
        result.externalURL == "https://s3-ca-central-1.amazonaws.com/photos.cloudcard/000002.23.236.icoAtl6vF16dhdvLD595GA91RhTco4e7Us4nuL22fA7o0069EqoP3i750e8QbIT3OqfVd28crlDEthuM.jpg"

        result.person.id == 944
        result.person.username == "tony+hg9@sharptop.co"
        result.person.email == "tony+hg9@sharptop.co"
        result.person.identifier == "770123"

        result.manuallyEdited
        result.dateCreatedString == "2023-08-24T20:28:28+0000"
        result.dateCreated == new Date(2023-1900, Calendar.AUGUST, 24, 20, 28, 28)
        result.version == 5
        result.backgroundReplaced
        result.helperBotReviewed
        result.aspectRatio == 1.0
        result.lowestClassification == 0.4954
    }
}
