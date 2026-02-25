package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue

@Service
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "IntegrationStorageService")
class IntegrationStorageService implements StorageService {

    static final Logger log = LoggerFactory.getLogger(IntegrationStorageService.class);

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    FileNameResolver fileNameResolver;

    @Autowired
    IntegrationStorageClient integrationStorageClient

    @PostConstruct
    void init() {
        throwIfTrue(fileNameResolver == null, "The File Name Resolver must be specified.")
        throwIfTrue(integrationStorageClient == null, "The Integration Client must be specified.")

        log.info("   File Name Resolver : $fileNameResolver.class.simpleName")
        log.info("  Integration Client : $integrationStorageClient.class.simpleName")
    }

    StorageResults save(Collection<Photo> photos) {
        StorageResults results = new StorageResults([])

        if (!photos) {
            log.info("No Photos to Upload")
            return results
        }

        log.info("Uploading Photos to ${integrationStorageClient.systemName}")

        photos.each {
            try {
                save(it)?.with { results.downloadedPhotoFiles.add(it) }
            } catch(FailedPhotoFileException failedPhotoFileException){
                results.failedPhotoFiles.add(FailedPhotoFile.fromPhotoId(it.id, failedPhotoFileException.message))
            }
        }

        integrationStorageClient.close()

        return results
    }

    PhotoFile save(Photo photo) {
        if (!photo.person) {
            log.error("Person does not exist for photo $photo.id and it cannot be uploaded to ${integrationStorageClient.systemName}.")
            return null
        }

        log.info("Uploading to ${integrationStorageClient.systemName}: $photo.id for person: $photo.person.email")

        String accountId = resolveAccountId(photo)

        if (!accountId || !photo.bytes) { return null }

        try {
            integrationStorageClient.putPhoto(accountId, photo.bytes)
        } catch (FailedPhotoFileException failedPhotoFileException) {
            throw failedPhotoFileException
        } catch (Exception e) {
            log.error("Photo $photo.id for $photo.person.email failed to upload into ${integrationStorageClient.systemName}.")
            log.error(e.message)
            e.printStackTrace()
            return null
        }

        return new PhotoFile(accountId, null, photo.id)
    }

    String resolveAccountId(Photo photo) {
        String accountId = fileNameResolver.getBaseName(photo);

        if (!accountId) {
            log.error("We could not resolve the accountId for '$photo.person.email' with ID number '$photo.person.identifier', so photo $photo.id cannot be uploaded to ${integrationStorageClient.systemName}.")
            return null
        }

        return accountId
    }

}
