package com.cloudcard.photoDownloader

import groovy.json.JsonSlurper
import jakarta.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import groovy.json.JsonOutput

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

import java.util.concurrent.ConcurrentHashMap
import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue

@Service
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "TouchNetStorageService")
class TouchNetStorageService implements StorageService {

    static final Logger log = LoggerFactory.getLogger(TouchNetStorageService.class);

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    FileNameResolver fileNameResolver;

    @Autowired
    TouchNetClient touchNetClient

    @Autowired
    CloudCardClient cloudCardClient

    String onHold = 'ON_HOLD'
    String backupMessage = 'Photo failed to save. Check if the user exists in TouchNet with the right identifier.'
    private static final int MAX_TRIES = 3
    private final ConcurrentHashMap<String, Integer> sessionAttempts = new ConcurrentHashMap<>()

    @PostConstruct
    void init() {
        throwIfTrue(fileNameResolver == null, "The File Name Resolver must be specified.");

        log.info("   File Name Resolver : $fileNameResolver.class.simpleName")
    }

    @Override
    StorageResults save(Collection<Photo> photos) {
        if (!photos) {
            log.info("No Photos to Upload")
            return StorageResults.empty()
        }

        log.info("Uploading Photos to TouchNet")

        String sessionId = touchNetClient.operatorLogin()
        if (!sessionId) {
            log.error("Failed to login to the TouchNet API")
            return StorageResults.empty()
        }

        List<PhotoFile> photoFiles = []
        List<FailedPhotoFile> failedPhotoFiles = []

        photos.each { photo ->
            try {
                PhotoFile saved = save(photo, sessionId)
                if (saved != null) {
                    photoFiles << saved
                } else {
                    failedPhotoFiles << new FailedPhotoFile(
                            null,
                            null,
                            photo.id,
                            "Failed to upload to TouchNet"
                    )
                }
            } catch (Exception e) {
                failedPhotoFiles << new FailedPhotoFile(
                        null,
                        null,
                        photo.id,
                        "Exception uploading to TouchNet: ${e.message}"
                )
            }
        }

        if (!touchNetClient.operatorLogout(sessionId)) {
            log.warn("Failed to logout of the TouchNet API")
        }

        return new StorageResults(photoFiles, failedPhotoFiles)
    }

    PhotoFile save(Photo photo, String sessionId) {
        if (!photo.person) {
            log.error("Person does not exist for photo $photo.id and it cannot be uploaded to TouchNet.")
            return null
        }

        log.info("Uploading to TouchNet: $photo.id for person: $photo.person.email")

        String accountId = resolveAccountId(photo)
        String photoBase64 = getBytesBase64(photo)

        if (!accountId || !photoBase64) {
            return null
        }

        TouchNetResponse response = touchNetClient.accountPhotoApprove(sessionId, accountId, photoBase64)
        String messageJson = JsonOutput.toJson(response)

        if (!response.success) {
            log.error("Photo $photo.id for $photo.person.email failed to upload into TouchNet.")

            int attempts = sessionAttempts.merge(sessionId, 1) { oldVal, one -> oldVal + one }

            if (attempts >= MAX_TRIES) {
                String message = extractMessage(messageJson) ?: backupMessage
                cloudCardClient.updateStatus(photo, onHold, message)
                sessionAttempts.remove(sessionId)
                return null
            }
            return null
        }
        sessionAttempts.remove(sessionId)
        return new PhotoFile(accountId, null, photo.id)
    }

    String resolveAccountId(Photo photo) {
        String accountId = fileNameResolver.getBaseName(photo);

        if (!accountId) {
            log.error("We could not resolve the accountId for '$photo.person.email' with ID number '$photo.person.identifier', so photo $photo.id cannot be uploaded to TouchNet.")
            return null
        }

        return accountId
    }

    String getBytesBase64(Photo photo) {
        if (!photo.bytes) {
            log.error("Photo $photo.id for $photo.person.email is missing binary data, so it cannot be uploaded to TouchNet.")
            return null
        }

        return Base64.getEncoder().encodeToString(photo.bytes)
    }

    static String extractMessage(String jsonResponse) {
        def parsed = new JsonSlurper().parseText(jsonResponse)
        return parsed.json?.ResponseStatus?.Message?.toString()
    }


}
