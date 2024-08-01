package com.cloudcard.photoDownloader

import com.fasterxml.jackson.databind.ObjectMapper
import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HttpClient {
    private static final Logger log = LoggerFactory.getLogger(HttpClient.class)

    String implementingClass

    private boolean debug = true

    ResponseWrapper makeRequest(String methodName, String actionType, String url, Map headers, String serializedBody = "") {

        Closure request = configureRequest(methodName, actionType, url, headers, serializedBody)
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
                log.info("${implementingClass ?: "Class not Specified"}: ${methodName}() Response: $response.status")
            }

        } catch (UnirestException e) { // ?
            log.error(e.message)
            wrapper = new ResponseWrapper(e)
        }

        if (debug) {
            log.info("DEBUG REQUEST - Response: " + new ObjectMapper().writeValueAsString(wrapper))
        }

        return wrapper
    }

    private Closure configureRequest(String methodName, String actionType, String url, Map headers, String serializedBody = "") {
        Closure request
        HttpResponse<String> response

        if (debug) {
            log.info("DEBUG REQUEST **************************************")
            log.info("DEBUG REQUEST - methodName: $methodName")
            log.info("DEBUG REQUEST - actionType: $actionType")
            log.info("DEBUG REQUEST - url: $url")
            log.info("DEBUG REQUEST - headers: $headers")
            log.info("DEBUG REQUEST - body: $serializedBody")
            log.info("END DEBUG REQUEST **********************************")
        }

        switch (actionType.toLowerCase()) {
            case "post":
                request = {
                    response = Unirest.post(url)
                            .headers(headers)
                            .body(serializedBody)
                            .asString()
                }
                break
            case "get":
                request = {
                    response = Unirest.get(url)
                            .headers(headers)
                            .asString()
                }
                break
            case "put":
                request = {
                    response = Unirest.put(url)
                            .headers(headers)
                            .body(serializedBody)
                            .asString()
                }
                break
            case "patch":
                request = {
                    response = Unirest.patch(url)
                            .headers(headers)
                            .body(serializedBody)
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
            default:
                log.error("HttpClient: ${methodName}() was passed invalid parameters.")

                request = null
        }

        return request
    }


}

class ResponseWrapper {
    UnirestException exception
    boolean success
    Object body
    int status

    ResponseWrapper(int code, String response = "No response body", boolean isSuccessful = false) {
        body = response
        status = code
        success = isSuccessful
    }

    ResponseWrapper(HttpResponse<String> response) {
        body = new JsonSlurper().parseText(response.body)
        status = response.status
        success = response.status >= 200 && response.status < 300
    }

    ResponseWrapper(UnirestException ex) {
        exception = ex
        status = 0
        success = false
        body = null
    }
}
