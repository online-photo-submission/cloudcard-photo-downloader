package com.cloudcard.photoDownloader

import groovy.json.JsonSlurper
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = 'Origo.useOrigo', havingValue = 'true')
class OrigoService {
    // Handles business logic / processing for incoming events and requests from API
    private static final Logger log = LoggerFactory.getLogger(OrigoService.class)

    @Value('${Origo.filterSet}')
    String filterSet

    @Autowired
    OrigoClient origoClient

    @Autowired
    OrigoEventStorageServiceLocal eventStorageServiceLocal

    @PostConstruct
    init() {
//        getNewAccessToken()


    }

    void getNewAccessToken() {
        OrigoResponse response = origoClient.authenticate()

        if (response.success) {
            String token = response.json.access_token
            origoClient.setAccessToken(token)
        } else {
            log.error("ORIGO: Cannot obtain access token")
        }
    }

    static List<Object> getEvents(String dateFrom, String dateTo = "", String filterId = "", String callbackStatus = "") {
//        DateTimeFormatter isoFormatter = DateTimeFormatter.ISO_DATE
//
//        String dateFromISO = isoFormatter(dateFrom)
//        String dateToISO
//        if (!dateTo) {
//            dateToISO = eventStorageServiceLocal.nowAsIsoFormat()
//        } else {
//            dateToISO = isoFormatter(dateTo)
//        }
//
//        def (result) = origoClient.listEvents(dateFromISO, dateToISO, filterId, callbackStatus)

        String resultJson = """
[
                {
                    "type": "com.origo.mi.user",
                    "specversion": "1.0",
                    "source": "https://dev.portal.origo.hidcloud.com/credential-management/customer/1003233/user/3456",
                    "id": "a1b2c3d4-ef56-7890-ab12-cd34ef567890",
                    "time": "2023-06-15T14:23:45.123Z",
                    "datacontenttype": "application/vnd.hidglobal.origo.events.user-2.0+json",
                    "data": {
                        "organization_id": "1003233",
                        "userId": "3456",
                        "firstName": "John",
                        "lastName": "Doe",
                        "status": "USER_CREATED",
                        "email": "john.doe@example.com"
                    }
                },{
                    "type": "com.origo.mi.user",
                    "specversion": "1.0",
                    "source": "https://dev.portal.origo.hidcloud.com/credential-management/customer/1003233/user/3456",
                    "id": "a1b2c3d4-ef56-7890-ab12-cd34ef567890",
                    "time": "2023-06-15T14:23:45.123Z",
                    "datacontenttype": "application/vnd.hidglobal.origo.events.user-2.0+json",
                    "data": {
                        "organization_id": "1003233",
                        "userId": "3456",
                        "firstName": "John",
                        "lastName": "Doe",
                        "status": "USER_CREATED",
                        "email": "john.doe@example.com"
                    }
                },{
                    "type": "com.origo.mi.user",
                    "specversion": "1.0",
                    "source": "https://dev.portal.origo.hidcloud.com/credential-management/customer/1003233/user/3456",
                    "id": "a1b2c3d4-ef56-7890-ab12-cd34ef567890",
                    "time": "2023-06-15T14:23:45.123Z",
                    "datacontenttype": "application/vnd.hidglobal.origo.events.user-2.0+json",
                    "data": {
                        "organization_id": "1003233",
                        "userId": "3456",
                        "firstName": "John",
                        "lastName": "Doe",
                        "status": "USER_CREATED",
                        "email": "john.doe@example.com"
                    }
                },{
                    "type": "com.origo.mi.user",
                    "specversion": "1.0",
                    "source": "https://dev.portal.origo.hidcloud.com/credential-management/customer/1003233/user/3456",
                    "id": "a1b2c3d4-ef56-7890-ab12-cd34ef567890",
                    "time": "2023-06-15T14:23:45.123Z",
                    "datacontenttype": "application/vnd.hidglobal.origo.events.user-2.0+json",
                    "data": {
                        "organization_id": "1003233",
                        "userId": "3456",
                        "firstName": "John",
                        "lastName": "Doe",
                        "status": "USER_CREATED",
                        "email": "john.doe@example.com"
                    }
                },
                {
                    "type": "com.origo.mi.user",
                    "specversion": "1.0",
                    "source": "https://dev.portal.origo.hidcloud.com/credential-management/customer/1003233/user/7890",
                    "id": "b2c3d4e5-f678-9012-ab34-cd56ef789012",
                    "time": "2024-02-28T08:12:34.567Z",
                    "datacontenttype": "application/vnd.hidglobal.origo.events.user-2.0+json",
                    "data": {
                        "organization_id": "1003233",
                        "userId": "7890",
                        "firstName": "Jane",
                        "lastName": "Smith",
                        "status": "USER_CREATED",
                        "email": "jane.smith@example.com"
                    }
                }
        ]
        """
        JsonSlurper jsonSlurper = new JsonSlurper()
        def result = jsonSlurper.parseText(resultJson) as List<Object>
        return result
    }

}
