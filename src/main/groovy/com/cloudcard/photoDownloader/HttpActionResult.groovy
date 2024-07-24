package com.cloudcard.photoDownloader

import com.mashape.unirest.http.exceptions.UnirestException

class HttpActionResult {
    def result

    HttpActionResult(ThirdPartyResponse resp) {
        result = resp
    }

    HttpActionResult(UnirestException exception) {
        result = exception
    }
}
