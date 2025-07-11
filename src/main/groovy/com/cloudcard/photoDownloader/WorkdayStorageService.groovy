package com.cloudcard.photoDownloader


import jakarta.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue

//TODO the only material differences between this and the TouchNetStorageService are the specific client being injected,
// and the specific session management logic used by touchnet. The session management logic could be moved to the client, and if the client
// provides the same interface, then this could be a single class with the client injected.
@Service
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "WorkdayStorageService")
class WorkdayStorageService implements StorageService {

    static final Logger log = LoggerFactory.getLogger(WorkdayStorageService.class);

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    FileNameResolver fileNameResolver;

    @Autowired
    WorkdayClient workdayClient

    @PostConstruct
    void init() {
        throwIfTrue(fileNameResolver == null, "The File Name Resolver must be specified.");

        log.info("   File Name Resolver : $fileNameResolver.class.simpleName")
    }

    List<PhotoFile> save(Collection<Photo> photos) {
        if (!photos) {
            log.info("No Photos to Upload")
            return []
        }

        log.info("Uploading Photos to Workday")

        List<PhotoFile> photoFiles = photos.findResults { save(it) }

        return photoFiles
    }

    PhotoFile save(Photo photo) {
        if (!photo.person) {
            log.error("Person does not exist for photo $photo.id and it cannot be uploaded to Workday.")
            return null
        }

        log.info("Uploading to Workday: $photo.id for person: $photo.person.email")

        String accountId = resolveAccountId(photo)
        String photoBase64 = getBytesBase64(photo)

        if (!accountId || !photoBase64) {
            return null
        }

        try {
            workdayClient.putWorkerPhoto(accountId, photoBase64)
        } catch (Exception e) {
            log.error("Photo $photo.id for $photo.person.email failed to upload into Workday.")
            return null
        }

        return new PhotoFile(accountId, null, photo.id)
    }

    String resolveAccountId(Photo photo) {
        String accountId = fileNameResolver.getBaseName(photo);

        if (!accountId) {
            log.error("We could not resolve the accountId for '$photo.person.email' with ID number '$photo.person.identifier', so photo $photo.id cannot be uploaded to Workday.")
            return null
        }

        return accountId
    }

    String getBytesBase64(Photo photo) {
        if (!photo.bytes) {
            log.error("Photo $photo.id for $photo.person.email is missing binary data, so it cannot be uploaded to Workday.")
            return null
        }

        return Base64.getEncoder().encodeToString(photo.bytes)
    }

}
