package com.cloudcard.photoDownloader.integrations.origo

import com.cloudcard.photoDownloader.ResponseWrapper
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

@Service
class OrigoService {

    private static final Logger log = LoggerFactory.getLogger(OrigoService.class)

    @Value('${Origo.filterSet}')
    String filterSet

    @Autowired
    OrigoClient origoClient

    private boolean getNewAccessToken() {
        ResponseWrapper response = origoClient.authenticate()
        boolean result

        if (response.success) {
            String token = response.body.access_token
            origoClient.setToken(token)
            result = true
        } else {
            log.error("ORIGO: Cannot obtain access token")
            result = false
        }

        return result
    }

    private List<Object> getCurrentFilters() {
        List<Object> filters = []
        ResponseWrapper response = origoClient.listFilters()
        log.info("ORIGOSERVICE Current Event Filters: " + response.body)

        if (response.success) {
            filters = response.body as List<Object>
        } else if (response.status == 401) {
            Closure command = { getCurrentFilters() }
            authenticateAndRetry(command, "getCurrentFilters")
        }

        return filters
    }

    private String createNewFilter(List<String> filterSet) {
        ResponseWrapper response = origoClient.createFilter(filterSet)
        String filterId = ""

        if (response.success) {
            filterId = response.body.filterId
        } else if (response.body.responseHeader.statusCode == 401) {
            Closure command = { createNewFilter(filterSet) }
            authenticateAndRetry(command, "createNewFilter")
        }

        return filterId
    }

    List<Object> getEvents(String dateFrom = "", String dateTo = "", String filterId = "", String callbackStatus = "") {
        ResponseWrapper response = origoClient.listEvents(dateFrom, dateTo, filterId, callbackStatus)
        ArrayList<Object> events = []
        if (response.success) {
            events = response.body as ArrayList<Object>
        } else if (response.status == 401) {
            Closure command = { getEvents(dateFrom, dateTo, filterId, callbackStatus) }
            authenticateAndRetry(command, "getEvents")
        }

        return events
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

    private static void processNewUsers(ArrayList<Object> events) {
        log.info("ORIGOSERVICE: Processing received events.")
        events.each {
            if (it.data.status == "USER_CREATED") {
                log.info("ORIGOSERVICE: Provisioning Origo user, $it.data.userId, in CloudCard API")
                log.info "************************************************************"
            }
        }
    }
}
