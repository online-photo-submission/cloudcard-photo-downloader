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

        if (!origoClient.isAuthenticated) {
            if (!origoClient.authenticate()) return []
        }

        List<PhotoFile> photoFiles = photos.collect { save(it) }.findAll { it != null }

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

        if (!fileType || (fileType != "png" && fileType != "jpg")) {
            log.error("Photo $photo.id for $photo.person.email has an invalid file type.")
            return null
        }

        ResponseWrapper upload = origoClient.uploadUserPhoto(photo, fileType)

        if (upload.status == 401) {
            origoClient.authenticate()
            save(photo)
            return null
        } else if (!upload.success) {
            log.error("Photo $photo.id for $photo.person.email failed to upload into Origo.")
            return null
        }

        String origoPhotoId = upload.body?.id
        ResponseWrapper approved = origoClient.accountPhotoApprove(photo, origoPhotoId)
        if (!approved.success) {
            log.error("Photo ${photo.id} for $photo.person.email was uploaded, but failed to be auto-approved.")
        }

        return new PhotoFile(accountId, null, photo.id)
    }

    String resolveFileType(Photo photo) {

        String fileType = ""
        if (photo.links.bytes && photo.links.bytes.length() > 3) {
            fileType = photo.links.bytes[-3..-1]
        } else {
            log.error("Could not resolve filetype for photo $photo.id for user $photo.person.email")
        }

        return fileType.toLowerCase()
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
