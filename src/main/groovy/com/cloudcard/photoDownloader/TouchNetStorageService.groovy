package com.cloudcard.photoDownloader;


class TouchNetStorageService implements StorageService {

    @Value("${TouchNetStorageService.apiUrl}")
    String apiUrl

    @Value("${TouchNetStorageService.developerKey}")
    String developerKey

    @Value("${TouchNetStorageService.operatorId}")
    String operatorId = "CloudCard"

    @Value("${TouchNetStorageService.operatorPassword}")
    String operatorPassword

    @Value("${TouchNetStorageService.terminalId}")
    String terminalId

    @Value("${TouchNetStorageService.terminalType}")
    String terminalType = "ThirdParty"

    @Value("${TouchNetStorageService.originId}")
    int originId


    List<PhotoFile> save(Collection<Photo> photos) throws Exception {
        TouchNetClient touchNetClient = new TouchNetClient(
                apiUrl: apiUrl,
                developerKey: developerKey,
                operatorId: operatorId,
                operatorPassword: operatorPassword,
                terminalId: terminalId,
                terminalType: terminalType,
                originId: originId
        )

        String sessionId = touchNetClient.operatorLogin()

        List<PhotoFile> photoFiles = photos.collect {
            //todo currently assuming that the identifier matches up to the touchnet account ID;
            //want to include custom field file name resolver so that we can match up by a custom field.
            String accountId = photo.person.identifier
            String photoBase64 = Base64.getEncoder().encode(photo.bytes)

            //this should throw an exception if it fails.
            //should we log antything here?
            touchNetClient.accountPhotoApprove(sessionId, accountId, photoBase64)

            return new PhotoFile(accountId, null, photo.id)
        }

        touchNetClient.operatorLogout(sessionId)

        return photoFiles
    }
}
