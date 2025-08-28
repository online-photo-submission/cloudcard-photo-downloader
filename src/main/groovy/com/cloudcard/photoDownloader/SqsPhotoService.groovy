package com.cloudcard.photoDownloader

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SqsException

import jakarta.annotation.PostConstruct

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

@Service
@ConditionalOnProperty(value = "downloader.photoService", havingValue = "SqsPhotoService", matchIfMissing = true)
class SqsPhotoService implements PhotoService {

    private static final Logger log = LoggerFactory.getLogger(SqsPhotoService.class)

    @Value('${sqsPhotoService.queueUrl}')
    String queueUrl

    @Value('${sqsPhotoService.pollingIntervalSeconds:0}')
    int pollingIntervalSeconds

    @Value('${sqsPhotoService.pollingDurationSeconds:20}')
    int pollingDurationSeconds

    @Value('${aws.sqs.region:ca-central-1}')
    String region

    @Value('${sqsPhotoService.updateStatus}')
    String updateStatus

    SqsClient sqsClient

    @Autowired
    RestService restService

    @Autowired
    PreProcessor preProcessor

    @Autowired
    CloudCardClient cloudCardClient;

    Map<Integer, Message> messageHistory = [:]

    @PostConstruct
    void init() {

        throwIfBlank(queueUrl, "The SQS Queue URL must be specified.")

        log.info("              SQS URL : " + queueUrl)
        log.info("           AWS Region : " + region)
        log.info("        Pre-Processor : " + preProcessor.getClass().getSimpleName())

        try {
            sqsClient = SqsClient.builder()
                .region(Region.of(region))
                .build()
        } catch(IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid AWS region specified: " + region, e)
        }
    }

    @Override
    long minDownloaderDelay() {
        return 0
    }

    @Override
    List<Photo> fetchReadyForDownload() {
        List<Photo> photos = []
        List<Message> messages = waitForMessages()
        messages.each {
            log.debug(it.body())
            Photo photo = Photo.fromSqsMessage(it)
            if (photo) {
                Photo processedPhoto = preProcessor.process(photo)
                restService.fetchBytes(processedPhoto)
                messageHistory[photo.id] = it
                photos += processedPhoto
            }
        }
        return photos
    }

    /**
     * Deletes the message associated with this photo from SQS
     * @param photo
     * @return photo
     */
    @Override
    Photo markAsDownloaded(PhotoFile photoFile) {
        Photo photo = new Photo(photoFile.getPhotoId());
        if (updateStatus && photoFile.isDownloaded()) {
            cloudCardClient.updateStatus(photo, updateStatus)
        }
        deleteMessages(sqsClient, queueUrl, messageHistory[photo.id])
        return photo
    }

    /* *** PRIVATE HELPERS *** */

    /**
     * Retrieves messages from SQS
     * @return
     */

    private List<Message> receiveMessages(SqsClient sqsClient, String queueUrl) {
        final int TARGET = 10
        List<Message> out = []
        int remaining = TARGET

        while (remaining > 0) {

            int wait = out.isEmpty() ? pollingDurationSeconds : 0

            ReceiveMessageRequest req = ReceiveMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .maxNumberOfMessages(Math.min(remaining, 10))
                    .waitTimeSeconds(wait)
                    .build() as ReceiveMessageRequest

            List<Message> batch = sqsClient.receiveMessage(req).messages()
            if (!batch || batch.isEmpty()) {
                break
            }

            out.addAll(batch)
            remaining = TARGET - out.size()
        }

        return out
    }

    static void deleteMessages(SqsClient sqsClient, String queueUrl, Message message) {
        try {
            DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
                    .queueUrl(queueUrl)
                    .receiptHandle(message.receiptHandle())
                    .build()
            sqsClient.deleteMessage(deleteMessageRequest)

        } catch (SqsException e) {
            log.error(e.awsErrorDetails().errorMessage())
            throw e
        }
    }

    private List<Message> waitForMessages() {
        List<Message> messages = []

        log.info("Waiting for SQS message")
        while (!messages) {
            sleep(pollingIntervalSeconds)
            print(".")
            messages = receiveMessages(sqsClient, queueUrl)
        }
        println("!")
        log.info("Received ${messages.size()} messages.")
        return messages
    }
}
