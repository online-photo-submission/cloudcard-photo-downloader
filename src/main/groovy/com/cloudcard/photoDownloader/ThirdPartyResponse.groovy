package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import groovy.json.JsonSlurper

class ThirdPartyResponse {
    boolean success
    Object json
    String jsonString

    ThirdPartyResponse() {}

    ThirdPartyResponse(HttpResponse<String> response) {
        json = new JsonSlurper().parseText(response.body)
        success = json.Status == "OK"
    }
}