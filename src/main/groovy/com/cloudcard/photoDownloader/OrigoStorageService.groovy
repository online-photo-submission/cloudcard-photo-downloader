package com.cloudcard.photoDownloader


import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue

@Service
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "OrigoStorageService")
class OrigoStorageService implements StorageService {

    static final Logger log = LoggerFactory.getLogger(OrigoStorageService.class)

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    FileNameResolver fileNameResolver

    @Autowired
    OrigoClient origoClient

    @PostConstruct
    void init() {
        throwIfTrue(fileNameResolver == null, "The File Name Resolver must be specified.")
        throwIfTrue(fileNameResolver == null, "The File Name Resolver must be specified.")

        log.info("   File Name Resolver : $fileNameResolver.class.simpleName")
    }

    List<PhotoFile> save(Collection<Photo> photos) {
        if (!photos) {
            log.info("No Photos to Upload")
            return []
        }

        log.info("Uploading Photos to Origo")

        String sessionId = origoClient.getAccessToken()

        if (!sessionId) {
            log.error("Failed to login to the Origo API")
            return []
        }

        List<PhotoFile> photoFiles = photos.collect { save(it, sessionId) }

        return photoFiles
    }

    PhotoFile save(Photo photo, String sessionId) {
        if (!photo.person) {
            log.error("Person does not exist for photo $photo.id and it cannot be uploaded to Origo.")
            return null
        }

        log.info("Uploading to Origo: $photo.id for person: $photo.person.email")

        String accountId = resolveAccountId(photo)
//        String photoBase64 = getBytesBase64(photo)
//
//        if (!accountId || !photoBase64) {
//            return null
//        }

        ResponseWrapper upload = origoClient.uploadUserPhoto(photo)

        if (!upload.success) {
            log.error("Photo $photo.id for $photo.person.email failed to upload into Origo.")
            return null
        }

        ResponseWrapper approved = origoClient.accountPhotoApprove(photo)

        if (!approved.success) {
            log.error("Photo $photo.id for $photo.person.email failed to be auto-approved.")
            return null
        }

        return new PhotoFile(accountId, null, photo.id)
    }

    String resolveAccountId(Photo photo) {
        String accountId = fileNameResolver.getBaseName(photo)

        if (!accountId) {
            log.error("We could not resolve the accountId for '$photo.person.email' with ID number '$photo.person.identifier', so photo $photo.id cannot be uploaded to Origo.")
            return null
        }

        return accountId
    }

    String getBytesBase64(Photo photo) {
        if (!photo.bytes) {
            log.error("Photo $photo.id for $photo.person.email is missing binary data, so it cannot be uploaded to Origo.")
            return null
        }

        return Base64.getEncoder().encodeToString(photo.bytes)
    }

}
