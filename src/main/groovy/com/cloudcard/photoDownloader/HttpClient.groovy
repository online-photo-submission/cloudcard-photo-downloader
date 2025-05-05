package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HttpClient {
    // This class can be extended for easy HTTP requests to a given API through the makeRequests() method. If you need to send custom requests with multiple body types such as json strings and files, it is recommended to build that request separately.

    private static final Logger log = LoggerFactory.getLogger(HttpClient.class)

    String extendingClass = "HttpClient"
    // used to inform logger of error source

    ResponseWrapper makeRequest(String actionType, String url, Map headers, String bodyString = "", byte[] bodyBytes = null) {
        // Provides an all-in-one http request builder which packages unirest client into a single method call

        if (bodyString && bodyBytes) {
            log.error("Cannot send string and file in same request.")
            return new ResponseWrapper(400)
        }

        Body body
        if (bodyString) body = new Body(bodyString)
        else if (bodyBytes) body = new Body(bodyBytes)
        else body = null

        Closure request = configureRequest(actionType, url, headers, body)
        ResponseWrapper wrapper

        try {
            wrapper = new ResponseWrapper(request())
        } catch (UnirestException e) { // ?
            log.error(e.message)
            wrapper = new ResponseWrapper(e)
        }

        return wrapper
    }

    private Closure configureRequest(String actionType, String url, Map headers, Body body = null) {

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

    void handleResponseLogging(String methodName, ResponseWrapper response, String customErrorMessage = "") {
        String standardResponseString = "$extendingClass - $methodName() Response status: $response.status"

        if (response.success) {
            log.info("$standardResponseString success")
        } else {
            log.error("$standardResponseString, ${customErrorMessage ?: response.body}")
        }
    }
}

class ResponseWrapper {
    UnirestException exception
    boolean success
    Object body
    int status

    static boolean isSuccessful(int code) {
        return code >= 200 && code < 300
    }

    ResponseWrapper(int code, String responseBody = "No response body") {
        body = responseBody
        status = code
        success = isSuccessful(code)
    }

    ResponseWrapper(HttpResponse<String> response) {
        body = response?.body ? new JsonSlurper().parseText(response.body) : "No response body."
        status = response.status
        success = isSuccessful(response.status)
    }

    ResponseWrapper(UnirestException e) {
        exception = e
        status = 500
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
