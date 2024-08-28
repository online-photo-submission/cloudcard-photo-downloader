package com.cloudcard.photoDownloader

import org.springframework.beans.factory.annotation.Value

import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank
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

    @Value('${Origo.overrideCurrentPhoto}')
    boolean overrideCurrentPhoto

    @PostConstruct
    void init() {
        throwIfTrue(fileNameResolver == null, "The File Name Resolver must be specified.")
        throwIfTrue(origoClient == null, "The Origo Client must be specified.")
        throwIfBlank("$overrideCurrentPhoto", "OverrideCurrentPhoto must be specified.")

        log.info("    File Name Resolver : $fileNameResolver.class.simpleName")
        log.info("          Origo Client : $origoClient.class.simpleName")
        log.info("Override current photo : ${overrideCurrentPhoto ? "True" : "False"}")
    }

    List<PhotoFile> save(Collection<Photo> photos) {
        if (!photos) {
            log.info("No Photos to Upload")
            return []
        }

        log.info("Uploading Photos to Origo")

        List<PhotoFile> photoFiles = photos.findResults { save(it) }

        return photoFiles
    }

    PhotoFile save(Photo photo) {
        if (!photo.person) {
            log.error("Person does not exist for photo $photo.id and it cannot be uploaded to Origo.")
            return null
        }

        log.info("Uploading to Origo: $photo.id for person: $photo.person.email")

        String accountId = resolveAccountId(photo)
        String fileType = resolveFileType(photo)

        if (!["png", "jpg"].contains(fileType)) {
            log.error("Photo $photo.id for $photo.person.email has an invalid file type.")
            return null
        }

        ResponseWrapper upload = origoClient.makeAuthenticatedRequest { origoClient.uploadUserPhoto(photo, fileType) }

        if (upload.status == 400 && overrideCurrentPhoto) {
            removeCurrentPhoto(photo)
            upload = origoClient.makeAuthenticatedRequest { origoClient.uploadUserPhoto(photo, fileType) }
        }
        if (!upload.success) {
            log.error("Photo $photo.id for $photo.person.email failed to upload into Origo.")
            return null
        }

        ResponseWrapper approved = origoClient.makeAuthenticatedRequest { origoClient.accountPhotoApprove(photo.person.identifier as String, upload.body?.id as String) }

        if (!approved.success) {
            log.error("Photo ${photo.id} for $photo.person.email was uploaded, but failed to be auto-approved.")
            return null
        }

        return new PhotoFile(accountId, null, photo.id)

    }

    void removeCurrentPhoto(Photo photo) {
        log.info("  =========== Override Current Photo Option Selected ===========")
        log.info("                --> Removing existing photo ID <--             ")
        ResponseWrapper details = origoClient.makeAuthenticatedRequest { origoClient.getUserDetails(photo.person.identifier) }

        if (details.success) {
            String photoId = details.body['urn:hid:scim:api:ma:2.2:User:Photo'].id[0]
            ResponseWrapper delete = origoClient.makeAuthenticatedRequest { origoClient.deletePhoto(photo.person.identifier, photoId) }
            if (!delete.success) log.error("Photo $photoId for $photo.person.email could not be deleted.")
        } else {
            log.error("Could not fetch details for $photo.person.email to remove existing photo.")
        }
    }

    String resolveFileType(Photo photo) {
        String fileType = ""

        String awsLink = photo.links?.bytes ?: ""

        if (awsLink.length() > 3) {
            fileType = photo.links.bytes[-3..-1].toLowerCase()
        } else {
            log.error("Could not resolve filetype for photo $photo.id for user $photo.person.email")
        }

        return fileType
    }

    String resolveAccountId(Photo photo) {
        String accountId = fileNameResolver.getBaseName(photo)

        if (!accountId) {
            log.error("We could not resolve the accountId for '$photo.person.email' with ID number '$photo.person.identifier', so photo $photo.id cannot be uploaded to Origo.")
            return null
        }

        return accountId
    }

}
