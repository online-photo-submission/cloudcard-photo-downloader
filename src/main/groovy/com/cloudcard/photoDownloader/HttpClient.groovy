package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HttpClient {
    // This class can be extended for easy HTTP requests to a given API through the makeRequests() method

    private static final Logger log = LoggerFactory.getLogger(HttpClient.class)

    String implementingClass
    // used to inform logger of error source

    ResponseWrapper makeRequest(String methodName, String actionType, String url, Map headers, String bodyString = "", byte[] bodyBytes = null) {
        // Provides an all-in-one http request builder which packages

        if (bodyString && bodyBytes) {
            log.info("Cannot send string and photo file in same request.")
            return new ResponseWrapper(400)
        }

        Body body
        if (bodyString) body = new Body(bodyString)
        else if (bodyBytes) body = new Body(bodyBytes)
        else body = null

        Closure request = configureRequest(methodName, actionType, url, headers, body)
        ResponseWrapper wrapper

        try {
            HttpResponse<String> response = request()

            if (response?.status < 200 || response?.status >= 300 || !response?.body) {
                if (response.status >= 500) {
                    throw new UnirestException("${implementingClass ?: "Class not Specified"}: ${methodName}() Error: status=${response?.status}, ${response?.body}")
                }
               wrapper = new ResponseWrapper(response.status, response.body)
            } else {
                wrapper = new ResponseWrapper(response)
                log.info("${implementingClass ?: "Class not Specified"}: ${methodName}() Response status: $response.status")
            }

        } catch (UnirestException e) { // ?
            log.error(e.message)
            wrapper = new ResponseWrapper(e)
        }

        return wrapper
    }

    private Closure configureRequest(String methodName, String actionType, String url, Map headers, Body body = null) {

        HttpResponse<String> response
        Closure request = {
            response = Unirest.get(url)
                    .headers(headers)
                    .asString()
        }

        switch (actionType.toLowerCase()) {
            case "post":
                request = {
                    response = Unirest.post(url)
                            .headers(headers)
                            .body(body.data)
                            .asString()
                }
                break
            case "put":
                request = {
                    response = Unirest.put(url)
                            .headers(headers)
                            .body(body.data)
                            .asString()
                }
                break
            case "patch":
                request = {
                    response = Unirest.patch(url)
                            .headers(headers)
                            .body(body.data)
                            .asString()
                }
                break
            case "delete":
                request = {
                    response = Unirest.delete(url)
                            .headers(headers)
                            .asString()
                }
                break
            case "get": // get is set as default
                break
        }

        return request
    }
}

class ResponseWrapper {
    UnirestException exception
    boolean success
    Object body
    int status

    ResponseWrapper(int code, String responseBody = "No response body", boolean isSuccessful = false) {
        body = responseBody
        status = code
        success = isSuccessful
    }

    ResponseWrapper(HttpResponse<String> response) {
        body = new JsonSlurper().parseText(response.body)
        status = response.status
        success = response.status >= 200 && response.status < 300
    }

    ResponseWrapper(UnirestException e) {
        exception = e
        status = 0
        success = false
        body = e.getMessage()
    }
}

class Body {
    Object data = null

    Body(String info) {
        data = info
    }

    Body(byte[] file) {
        data = file
    }
}
