//file:noinspection SpellCheckingInspection
package com.cloudcard.photoDownloader.storage.integration.ccure

import com.cloudcard.photoDownloader.*
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
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

    List<CCurePersonnel> newPersonnelQueue
    List<String> personnelJustCreated


    @PostConstruct
    void init() {
        newPersonnelQueue = Collections.synchronizedList(new ArrayList<CCurePersonnel>())
        personnelJustCreated = Collections.synchronizedList(new ArrayList<String>())

        if (createCCurePersonnel) {
            throwIfBlank(firstNameField, "The custom field name for first name must be provided in CCureClient.createPersonnel.firstName")
            throwIfBlank(lastNameField, "The custom field name for first name must be provided in CCureClient.createPersonnel.lastName")
        }
        cCureClient.authenticate()
        String startUpTime = lastRunPropertyService.getCurrentTimestamp()

        cCureClient.queryAuditLogsForNewPeople().each {{
            pushPhoto(it)
            lastRunPropertyService.updateLastRunTimestamp(it.timestamp)
        }}

        // reset to right before we started processing the backlog
        lastRunPropertyService.updateLastRunTimestamp(startUpTime)

        cCureClient.subscribeForNewEvents((payload) -> {
            if (payload.NotificationType == "ObjectCreated") {
                if (!personnelJustCreated.remove(payload.NotificationDSO.EmailAddress)) {
                    log.info("Received notification of personnel creation for ${payload.NotificationDSO.EmailAddress}");
                    CCurePersonnel newPersonnel = new CCurePersonnel(
                            id: payload.NotificationDSO.ObjectID,
                            emailAddress: payload.NotificationDSO.EmailAddress,
                            employeeId: payload.NotificationDSO[employeeIdField],
                            timestamp: lastRunPropertyService.getCurrentTimestamp()
                    )
                    newPersonnelQueue << newPersonnel
                }
            }
        })
    }

    @Override
    String getSystemName() {
        return "CCURE"
    }

    /*
    This maintains a single thread to process new personnel notifications one at a time, so we don't overwhelm the
     CCURE API and get throttled by lots of threads. All the threads just dump new people into the list.
     Now we have a max of two threads calling the API: 1 from notifications, and 1 from downloader processing.
     Everything on each side just queues up and processes in line.
     This runs with a minute of downtime after emptying the list.
     */
    @Scheduled(fixedDelay = 60_000)
    void processNewPersonnelQueue() {
        while (!newPersonnelQueue.empty) {
            pushPhoto(newPersonnelQueue.get(0))
            CCurePersonnel personnel = newPersonnelQueue.remove(0)
            lastRunPropertyService.updateLastRunTimestamp(personnel.timestamp)
        }
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
                putPhotos(cCurePersonnel, photo)
            } else if (createCCurePersonnel) {
                personnelJustCreated.add(photo.person.email)
                Long id = cCureClient.createPersonnel(photo.person.customFields?[firstNameField], photo.person.customFields?[lastNameField], photo.person.email, identifier)
                if (!id) {
                    throw new FailedPhotoFileException("Unable to create CCURE personnel record for $photo.person.email")
                }
                cCurePersonnel = cCureClient.getPersonnelDetails(identifier, photo.person.email)
                putPhotos(cCurePersonnel, photo)
            } else {
                throw new FailedPhotoFileException("CCURE Personnel record not found for $photo.person.email")
            }
        } catch (FailedPhotoFileException ex) {
            throw ex
        } catch (Exception ex) {
            log.error("Error while posting photo to CCURE: $ex.localizedMessage")
        }

    }

    // Stores the primary photo, and a signature if we have one
    void putPhotos(CCurePersonnel cCurePersonnel, Photo photo) {
        //Store the first photo
        cCureClient.storePhoto(cCurePersonnel.id, photo.bytesBase64, cCurePersonnel.partitionId, true)

        List<AdditionalPhoto> additionalPhotoList = cloudCardClient.getAdditionalPhotos(photo.person.id, "DRAW")

        additionalPhotoList.each {
            restService.fetchBytes(it)
            String bytesBase64 = Base64.getEncoder().encodeToString(it.bytes)
            log.info "Uploading signature for ${cCurePersonnel.id}"
            cCureClient.storePhoto(cCurePersonnel.id, bytesBase64, cCurePersonnel.partitionId, false)
        }
    }

    @Override
    void close() {
        // Not implemented: Connection kept open to receive notifications of new people
        // The shutdown method will close long-running connections
    }
}
