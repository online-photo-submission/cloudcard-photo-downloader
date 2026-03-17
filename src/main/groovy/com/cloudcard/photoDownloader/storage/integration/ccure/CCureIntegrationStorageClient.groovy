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

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfBlank

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

    @Value('${CCureClient.createPersonnel.firstName}')
    String firstNameField

    @Value('${CCureClient.createPersonnel.lastName}')
    String lastNameField

    @Value('${CCureClient.createRemotePhotoPerson:false}')
    boolean createRemotePhotoPerson

    @Value('${CCureClient.employeeIdField}')
    String employeeIdField

    @Autowired
    LastRunPropertyService lastRunPropertyService


    @PostConstruct
    void init() {
        if (createCCurePersonnel) {
            throwIfBlank(firstNameField, "The custom field name for first name must be provided in CCureClient.createPersonnel.firstName")
            throwIfBlank(lastNameField, "The custom field name for first name must be provided in CCureClient.createPersonnel.lastName")
        }
        cCureClient.authenticate()

        cCureClient.queryAuditLogsForNewPeople().each {pushPhoto(it)}

        cCureClient.subscribeForNewEvents((payload) -> {
            if (payload.NotificationType == "ObjectCreated") {
                log.info("Received notification of personnel creation for ${payload.NotificationDSO.EmailAddress}");
                CCurePersonnel newPersonnel = new CCurePersonnel(
                        id: payload.NotificationDSO.ObjectID,
                        emailAddress: payload.NotificationDSO.EmailAddress,
                        employeeId: payload.NotificationDSO[employeeIdField]
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
        String runTimestamp = lastRunPropertyService.getCurrentTimestamp()

        // load person by email
        log.trace("Loading cloudcard record for $personnel.emailAddress")
        Person cloudCardPerson = findCloudCardPerson(personnel)
        if (cloudCardPerson?.additionalProperties?.currentPhoto) {
            log.trace "Person $personnel.emailAddress has a photo, sending to CCURE"
            Photo photo = cloudCardPerson.additionalProperties.currentPhoto as Photo
            if (Photo.APPROVED_STATUSES.contains(photo.status)) {
                restService.fetchBytes(photo)
                putPhoto(personnel.employeeId as String, photo)
            }
        } else if (!cloudCardPerson && createRemotePhotoPerson) {
            log.trace "$personnel.emailAddress does not exist in RemotePhoto, creating record there"
            cloudCardClient.createPerson(personnel.emailAddress, personnel.employeeId)
        }

        lastRunPropertyService.updateLastRunTimestamp(runTimestamp)
    }

    Person findCloudCardPerson(CCurePersonnel personnel) {
        Person cloudCardPerson = null
        if (personnel.employeeId) {
            cloudCardPerson = cloudCardClient.findPersonByIdentifier(personnel.employeeId)
        }
        if (!cloudCardPerson) {
            cloudCardPerson = cloudCardClient.findPersonByEmail(personnel.emailAddress)
        }

        return cloudCardPerson
    }

    @Override
    void putPhoto(String identifier, Photo photo) {
        try {
            CCurePersonnel cCurePersonnel = cCureClient.getPersonnelDetails(identifier, photo.person.email)
            if (cCurePersonnel?.id) {
                cCureClient.storePhoto(cCurePersonnel.id, photo.bytesBase64, cCurePersonnel.partitionId, true)
                // TODO: store signature here
            } else if (createCCurePersonnel) {
                Long id = cCureClient.createPersonnel(photo.person.customFields?[firstNameField], photo.person.customFields?[lastNameField], photo.person.email, identifier)
                if (!id) {
                    throw new FailedPhotoFileException("Unable to create CCURE personnel record for $photo.person.email")
                }
                cCurePersonnel = cCureClient.getPersonnelDetails(identifier, photo.person.email)
                cCureClient.storePhoto(id, photo.bytesBase64, cCurePersonnel.partitionId, true)
                // TODO: store signature here
            } else {
                throw new FailedPhotoFileException("CCURE Personnel record not found for $photo.person.email")
            }
        } catch (FailedPhotoFileException ex) {
            throw ex
        } catch (Exception ex) {
            log.error("Error while posting photo to CCURE: $ex.localizedMessage")
        }

    }

    @Override
    void close() {
        // Not implemented: Connection kept open to receive notifications of new people
        // The shutdown method will close long-running connections
    }
}
