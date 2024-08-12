package com.cloudcard.photoDownloader.integrations


import com.cloudcard.photoDownloader.Person
import com.cloudcard.photoDownloader.ResponseWrapper
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
            log.error("Cannot obtain access token")
            result = false
        }

        return result
    }

    String provisionPerson(Person person) {
        ResponseWrapper response = cloudCardClient.createPerson(person)
        String personId = ""

        if (response.success) {
            personId = response.body.id.toString()
        } else if (response.status == 401 || response.status == 401) {
            Closure command = { provisionPerson(person) }
            authenticateAndRetry(command, "provisionPerson")
        }

        return personId
    }

    private void authenticateAndRetry(Closure command, String commandName) {
        boolean tokenSaved = getNewAccessToken()
        if (tokenSaved) {
            log.info("Retrying ${commandName}() with valid access token.")
            command()
        } else {
            log.error("Cannot authenticate.")
        }
    }
}
