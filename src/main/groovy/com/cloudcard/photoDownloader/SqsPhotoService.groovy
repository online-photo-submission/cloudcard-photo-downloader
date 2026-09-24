package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsClient
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import software.amazon.awssdk.services.sqs.model.SqsException

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

    @Value('${sqsPhotoService.region:${aws.sqs.region:ca-central-1}}')
    String region

    @Value('${sqsPhotoService.putStatus}')
    String putStatus

//  Fallback option so customers on API versions prior to STS can still use SQS if desired.
    @Value('${sqsPhotoService.authType:sts}')
    String authType

    SqsClient sqsClient

    @Autowired
    PreProcessor preProcessor

    @Autowired
    CloudCardClient cloudCardClient

    AwsCredentialsProvider credentialsProvider
    Map<Integer, Message> messageHistory = [:]

    @PostConstruct
    void init() {

        throwIfBlank(queueUrl, "The SQS Queue URL must be specified.")

        log.info("                    SQS URL : " + queueUrl)
        log.info("                 AWS Region : " + region)
        log.info("              Pre-Processor : " + preProcessor.getClass().getSimpleName())
        log.info("                 Put Status : " + putStatus)

        if (cloudCardClient.isConfigured() && authType == "sts") {
            credentialsProvider = new StsTokenRefreshingProvider(cloudCardClient, queueUrl)
            log.info("            SQS Credentials : brokered by the CloudCard API")
        } else {
            credentialsProvider = DefaultCredentialsProvider.create()
            log.warn("            SQS Credentials : using the AWS default credentials chain. Brokered credentials " +
                     "are strongly preferred. Set cloudcard.api.accessToken and set sqsPhotoService.authType=sts to migrate.")
        }

        try {
            sqsClient = SqsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(credentialsProvider)
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
                cloudCardClient.fetchBytes(processedPhoto)
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
    Photo markAsDownloaded(Photo photo) {
        if (putStatus) {
            cloudCardClient.updateStatus(photo, putStatus)
        }
        deleteMessages(sqsClient, queueUrl, messageHistory[photo.id])
        return photo
    }

    @Override
    Photo markAsFailed(Photo photo, String errorMessage) {
        deleteMessages(sqsClient, queueUrl, messageHistory[photo.id])

        if (cloudCardClient.isConfigured()) {
            cloudCardClient.updateStatus(photo, CloudCardClient.FAILED, errorMessage)
        }
        return photo
    }

//    TODO: Confirm that this continues to work after a token expires (can set expiry manually in the database), but I don't think we should close this with SQS.
    @Override
    void close() {
//        cloudCardClient.close()
    }

    /* *** PRIVATE HELPERS *** */

    /**
     * Retrieves messages from SQS
     * @return
     */

    private List<Message> receiveMessages(SqsClient sqsClient, String queueUrl) {
        ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(queueUrl)
                .maxNumberOfMessages(10)
                .waitTimeSeconds(pollingDurationSeconds)
                .build() as ReceiveMessageRequest
        return sqsClient.receiveMessage(receiveMessageRequest).messages()
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
        log.info("Recived ${messages.size()} messages.")
        return messages
    }
}
