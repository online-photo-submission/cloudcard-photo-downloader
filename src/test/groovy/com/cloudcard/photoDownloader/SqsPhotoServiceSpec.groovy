package com.cloudcard.photoDownloader

import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse
import spock.lang.Specification

class SqsPhotoServiceSpec extends Specification {

    SqsPhotoService service
    String messageBody = PhotoSpec.messageBody

    def setup() {
        println "setting up"
        service = new SqsPhotoService()
        service.preProcessor = Mock(PreProcessor)
        service.restService = Mock(RestService)
        service.sqsClient = Mock(SqsClient)
    }

    def "test fetch ready for download"() {
        given: "setup some mocks"
        Message mockMessage = GroovyMock(Message) {
            body() >> messageBody
        }

        ReceiveMessageResponse mockReceiveMessageResponse = GroovyMock(ReceiveMessageResponse) {
            messages() >> [mockMessage]
        }
        assert mockReceiveMessageResponse.messages().first().body() == messageBody

        and: "set up the photo"
        Photo photo = Photo.fromSqsMessage(mockMessage)

        when:
        List<Photo> photos = service.fetchReadyForDownload()

        then:
        photos.first() == photo

        and:
        1 * service.sqsClient.receiveMessage(_) >> mockReceiveMessageResponse
        1 * service.preProcessor.process(photo) >> photo
        1 * service.restService.fetchBytes(photo)
        service.messageHistory[photo.id].body() == messageBody
    }
}
