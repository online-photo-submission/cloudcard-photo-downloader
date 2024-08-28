package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import groovy.json.JsonException
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class HttpClient {
    private static final Logger log = LoggerFactory.getLogger(HttpClient.class)

    String source = "HttpClient"
    // used to inform logger of error source. Put "httpClient.source = this.class.name" in init() method

    String handleResponseLogging(String methodName, ResponseWrapper response, String customErrorMessage = "") {
        String standardResponseString = "$source - $methodName() Response status: $response.status"
        String result

        if (response.success) {
            result = "$standardResponseString success"
            log.info(result)
        } else {
            result = "$standardResponseString, ${customErrorMessage ?: response.body}"

            log.error(result)
        }

        return result
    }
}
