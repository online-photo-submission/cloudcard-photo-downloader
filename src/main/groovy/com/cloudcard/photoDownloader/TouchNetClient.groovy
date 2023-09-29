package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest
import groovy.json.JsonSlurper;

class TouchNetClient {

    String apiUrl
    String developerKey
    String operatorId
    String operatorPassword
    String terminalId
    String terminalType
    int originId

    boolean apiOnline() {
        HttpResponse<String> response = Unirest.get("$apiUrl/").asString();
        return response.status == 200
    }

    String operatorLogin() {
        Map request = [
            OperatorId: operatorId,
            PIN: operatorPassword,
            TerminalID: terminalId,
            OriginID: originId,
            TerminalType: terminalType
        ]

        String requestBody = new ObjectMapper().writeValueAsString(request)
        HttpResponse<String> response = Unirest.post("$apiUrl/operator/login")
                .header("Content-Type", "application/json")
                .header("DevKey", developerKey)
                .body(requestBody)
                .asString() // todo make response json and map to an object.
        println response.status
        println response.body

        def json = new JsonSlurper().parseText(response.body)
        boolean success = json.Status == "OK"
        if (!success) {
            log.error("OperatorLogin Failed: $response.body")
            return null;
        }

        return json.Result
    }

    boolean operatorLogout(String sessionId) {
        Map request = [
                SessionID: sessionId,
                OperatorId: operatorId
        ]

        String jsonBody = new ObjectMapper().writeValueAsString(request)
        println jsonBody
        HttpResponse<String> response = Unirest.post("$apiUrl/operator/logout")
                .header("Content-Type", "application/json")
                .header("DevKey", developerKey)
                .body(jsonBody) //todo make json
                .asString() // todo make json and map to an object.
        println response.status
        println response.body

        def json = new JsonSlurper().parseText(response.body)
        boolean success = json.Status == "OK"
        if (!success) {
            log.error("OperatorLogout Failed: $response.body")
        }
        return success
    }

    boolean accountPhotoApprove(String sessionId, String accountId, String photoBase64) {
        Map request = [
            SessionID: sessionId,
            AccountID: accountId,
            PhotoBase64: photoBase64,
            // TODO WHAT IS THIS?>>??>>??>>~ I'm assuming we don't need to care about it, btu what is it?
            ForcePrintedFlag: false
        ]

        String jsonBody = new ObjectMapper().writeValueAsString(request)
        println jsonBody
        HttpResponse<String> response = Unirest.post("$apiUrl/account/photo/approve")
                //todo abstract the basics into a function that we can call that inserts thte devkey, the json content type header, converts a map to json,
                //and throws the exception in case of failure, and returns the response content as a json parsed into a map.
                .header("Content-Type", "application/json")
                .header("DevKey", developerKey)
                .body(jsonBody) //todo make json
                .asString() // todo make json and map to an object.
        println response.status
        println response.body

        def json = new JsonSlurper().parseText(response.body)
        boolean success = json.Status == "OK"
        if (!success) {
            log.error("AccountPhotoApproveRequest Failed: $response.body")
        }
        return success
    }
}
