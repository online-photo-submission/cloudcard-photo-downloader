package com.cloudcard.photoDownloader

import com.mashape.unirest.http.exceptions.UnirestException
import org.springframework.beans.factory.annotation.Value

class HttpActionResult {
    // Wrapper for either http responses or exceptions, allowing for http calls to be placed in try/catch blocks

    ThirdPartyResponse success = null
    UnirestException error = null

    HttpActionResult() {}

    HttpActionResult(ThirdPartyResponse response) {
        success = response
    }

    HttpActionResult(UnirestException exception) {
        error = exception
    }
}
