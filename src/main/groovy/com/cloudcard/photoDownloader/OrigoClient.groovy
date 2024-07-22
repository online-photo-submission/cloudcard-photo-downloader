package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.JsonNode
import com.mashape.unirest.http.Unirest
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service


@Service
class OrigoClient {
    // Makes requests to Origo API

    @Value('${Origo.MA_URI}')
    private String apiUri

    @Value('${Origo.Authorization}')
    private String authorization

    @Value('${Origo.Content-Type}')
    private String contentType

    @Value('${Origo.Application-Version}')
    private String applicationVersion

    @Value('${Origo.Application-ID}')
    private String applicationId

    Map requestHeaders

    @PostConstruct
    def init() {
        requestHeaders = [
                'Authorization' : authorization,
                'Content-Type' : contentType,
                'Application-Version' : applicationVersion,
                'Application-ID' : applicationId
        ]

        getAllUsers()
    }

    def getAllUsers() {
        // Initial action of OrigoIntegration application - Requests all users for given organization.

        for (header in requestHeaders) {
            if (!header.value) {
                println "Error: Request is missing necessary headers."
                return
            }
        }

        List<Person> people
        HttpResponse<JsonNode> response

        try {
            response = Unirest.get(apiUri)
                    .headers(requestHeaders)
                    .asJson()

            if (response.status != 200) {
                println "Error: " + response.body
            }

            println "Response: $response.statusText; $response.status; $response.body"

        } catch (Exception e) {
            println e.message
        }
    }

    static def storePhoto() {
        // Stores photo in Origo system
    }

    static def storePersonData() {
        // stores 'customFields' information in Origo employee record
    }
}
