package com.cloudcard.photoDownloader


import javax.annotation.PostConstruct

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value

class TouchNetStorageService implements StorageService {

    static final Logger log = LoggerFactory.getLogger(TouchNetStorageService.class);

    @Value('${TouchNetStorageService.apiUrl}')
    String apiUrl

    @Value('${TouchNetStorageService.developerKey}')
    String developerKey

    @Value('${TouchNetStorageService.operatorId:CloudCard}')
    String operatorId

    @Value('${TouchNetStorageService.operatorPassword}')
    String operatorPassword

    @Value('${TouchNetStorageService.terminalId}')
    String terminalId

    @Value('${TouchNetStorageService.terminalType:ThirdParty}')
    String terminalType

    @Value('${TouchNetStorageService.originId}')
    int originId

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private FileNameResolver accountIdResolver;

    private TouchNetClient touchNetClient

    @PostConstruct
    void init() {

        throwIfTrue(accountIdResolver == null, "The File Name Resolver must be specified.");

        log.info("   File Name Resolver : $accountIdResolver.class.simpleName")

        touchNetClient = new TouchNetClient(
                apiUrl: apiUrl,
                developerKey: developerKey,
                operatorId: operatorId,
                operatorPassword: operatorPassword,
                terminalId: terminalId,
                terminalType: terminalType,
                originId: originId
        )

    }

    List<PhotoFile> save(Collection<Photo> photos) throws Exception {
        log.info("Uploading Photos to TouchNet")

        String sessionId = touchNetClient.operatorLogin()

        if (!sessionId) {
            log.error("Failed to login to the TouchNet API")
            return []
        }

        List<PhotoFile> photoFiles = photos.collect { Photo photo ->
            if (!photo.person) {
                log.error("Person does not exist for photo $photo.id and it cannot be uploaded to TouchNet.")
                return null
            }

            log.info("Uploading to TouchNet: $photo.id for person: $photo.person.email")

            String accountId = accountIdResolver.getBaseName(photo);

            if (!accountId) {
                log.error(
                        "We could not resolve the accountId for '$photo.person.email'" +
                        " with ID number '$photo.person.identifier'," +
                        " so photo $photo.id cannot be uploaded to TouchNet."
                )
                return null
            }

            if (!photo.bytes) {
                log.error("Photo $photo.id for $photo.person.email is missing binary data, so it cannot be uploaded to TouchNet.")
                return null
            }

            String photoBase64 = Base64.getEncoder().encodeToString(photo.bytes)

            if (!touchNetClient.accountPhotoApprove(sessionId, accountId, photoBase64)) {
                log.error("Photo $photo.id for $photo.person.email failed to upload into TouchNet.")
                return null
            }

            return new PhotoFile(accountId, null, photo.id)
        }

        if (!touchNetClient.operatorLogout(sessionId)) {
            log.error("Failed to logout of the TouchNet API")
        }

        return photoFiles
    }
}
