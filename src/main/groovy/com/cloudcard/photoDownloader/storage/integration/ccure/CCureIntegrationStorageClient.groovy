//file:noinspection SpellCheckingInspection
package com.cloudcard.photoDownloader.storage.integration.ccure

import com.cloudcard.photoDownloader.*
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(value = "IntegrationStorageService.client", havingValue = "CCureClient")
class CCureIntegrationStorageClient implements IntegrationStorageClient {

    static final Logger log = LoggerFactory.getLogger(CCureIntegrationStorageClient.class)

    @Autowired
    CCureClient cCureClient

    @Autowired
    CloudCardClient cloudCardClient

    @Autowired
    RestService restService

    @Value('${CCureClient.createPersonnel:false}')
    boolean createCCurePersonnel

    @Value('${CCureClient.employeeIdField}')
    String employeeIdField

    @Autowired
    LastRunPropertyService lastRunPropertyService


    @PostConstruct
    void init() {
        cCureClient.authenticate()

        cCureClient.queryAuditLogsForNewPeople().each {pushPhoto(it)}

        cCureClient.subscribeForNewEvents((payload) -> {
            if (payload.NotificationType == "ObjectCreated") {
                log.info("Received notification of personnel creation for ${payload.NotificationDSO.EmailAddress}");
                CCurePersonnel newPersonnel = new CCurePersonnel(
                        id: payload.NotificationDSO.ObjectID,
                        emailAddress: payload.NotificationDSO.EmailAddress
                )
                pushPhoto(newPersonnel)
            }
        })
    }

    @Override
    String getSystemName() {
        return "CCURE"
    }

    /**
     * Given data on a person from CCURE, this looks for a matching person (by email) within CloudCard.
     * If they have a record and an approved photo, we push the photo to CCURE.
     * If they don't have a CloudCard record, we make one.
     * @param personnel
     */
    void pushPhoto(CCurePersonnel personnel) {
        // load person by email
        log.trace("Loading cloudcard record for $personnel.emailAddress")
        Person cloudCardPerson = cloudCardClient.findPerson(personnel.emailAddress)
        if (cloudCardPerson?.additionalProperties?.currentPhoto) {
            log.trace "Person $personnel.emailAddress has a photo, sending to CCURE"
            Photo photo = cloudCardPerson.additionalProperties.currentPhoto as Photo
            if (Photo.APPROVED_STATUSES.contains(photo.status)) {
                restService.fetchBytes(photo)
                putPhoto(personnel.id as String, photo)
            }
        } else if (!cloudCardPerson) {
            log.trace "$personnel.emailAddress does not exist in RemotePhoto, creating record there"
            cloudCardClient.createPerson(personnel.emailAddress)
        }

        lastRunPropertyService.updateLastRunTimestamp()
    }

    @Override
    void putPhoto(String identifier, Photo photo) {
        try {
            // CCURE won't use the local identifier, we need to look up their CCURE id
            CCurePersonnel cCurePersonnel = cCureClient.getPersonnelDetailsByEmail(photo.person.email)
            if (cCurePersonnel?.id) {
                cCureClient.storePhoto(cCurePersonnel.id, photo.bytesBase64)
            } else if (createCCurePersonnel) {
                Long id = cCureClient.createPersonnel(photo.person.customFields?.firstName, photo.person.customFields?.lastName, photo.person.email)
                if (!id) {
                    throw new FailedPhotoFileException("Unable to create CCURE personnel record for $photo.person.email")
                }
                cCureClient.storePhoto(id, photo.bytesBase64)
            } else {
                throw new FailedPhotoFileException("CCURE Personnel record not found for $photo.person.email")
            }
        } catch (FailedPhotoFileException ex) {
            throw ex
        } catch (Exception ex) {
            log.error("Error while posting photo to CCURE: $ex.localizedMessage")
            throw new FailedPhotoFileException("Unable to process record in CCURE.")
        }

    }

    @Override
    void close() {
        // Not implemented: Connection kept open to receive notifications of new people
        // The shutdown method will close long-running connections
    }
}
