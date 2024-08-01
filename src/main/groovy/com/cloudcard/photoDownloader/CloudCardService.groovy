package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class CloudCardService {
    private static final Logger log = LoggerFactory.getLogger(CloudCardService.class)

    @Autowired
    CloudCardClient cloudCardClient

    @PostConstruct
    init() {

    }

    private boolean getNewAccessToken() {
        ResponseWrapper response = cloudCardClient.authenticate()
        boolean result

        if (response.success) {
            String token = response.body.tokenValue
            cloudCardClient.setToken(token)
            result = true
        } else {
            log.error("CloudCard: Cannot obtain access token")
            result = false
        }

        return result
    }

    private String createNewPerson(Person person) {
        ResponseWrapper response = cloudCardClient.provisionPerson(person)
        String personId = ""

        if (response.success) {
            personId = response.body.id.toString()
        } else if (response.status == 401) {
            Closure command = { createNewPerson(person) }
            authenticateAndRetry(command, "createNewPerson")
        }

        return personId
    }

    private void authenticateAndRetry(Closure command, String commandName) {
        boolean tokenSaved = getNewAccessToken()
        if (tokenSaved) {
            log.info("ORIGOSERVICE: Retrying ${commandName}() with valid access token.")
            command()
        } else {
            log.error("ORIGOSERVICE: Cannot authenticate.")
        }
    }
}
