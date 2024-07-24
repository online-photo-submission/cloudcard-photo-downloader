package com.cloudcard.photoDownloader

import com.mashape.unirest.http.exceptions.UnirestException
import org.springframework.beans.factory.annotation.Value

class HttpActionResult {
    // Wrapper for either http responses or exceptions, allowing for http calls to be placed in try/catch blocks

    @Value(null)
    def result

    HttpActionResult() {}

    HttpActionResult(ThirdPartyResponse response) {
        result = response
    }

    HttpActionResult(UnirestException exception) {
        result = exception
    }
}
