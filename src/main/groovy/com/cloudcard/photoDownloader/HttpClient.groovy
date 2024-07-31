package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import com.mashape.unirest.http.exceptions.UnirestException
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class HttpClient {
    private static final Logger log = LoggerFactory.getLogger(HttpClient.class)

    static ResponseWrapper makeRequest(String methodName, String actionType, String url, Map headers, String serializedBody = "") {

        Closure request = configureRequest(methodName, actionType, url, headers, serializedBody)
        ResponseWrapper wrapper

        try {
            HttpResponse<String> response = request()
            wrapper = new ResponseWrapper(response)

            if (!wrapper.success) {
                log.error("ORIGOCLIENT ${methodName}() Error: status=${response.status}, ${response.body}")
            } else {
                log.info("ORIGOCLIENT ${methodName}() Response: $response.status")
            }

        } catch (UnirestException e) { // ?
            log.error(e.message)
            wrapper = new ResponseWrapper(e)
        }

        return wrapper
    }

    private static Closure configureRequest(String methodName, String actionType, String url, Map headers, String serializedBody = "") {
        Closure request
        HttpResponse<String> response
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
                log.error("Bad request: ${methodName} was passed invalid parameters.")

                request = null
        }

        return request
    }
}

class ResponseWrapper {
    UnirestException exception
    boolean success
    Object body

    ResponseWrapper() {}

    ResponseWrapper(HttpResponse<String> response) {
        body = new JsonSlurper().parseText(response.body)
        success = response.status >= 200 && response.status < 300
    }

    ResponseWrapper(UnirestException ex) {
        exception = ex
        success = false
    }
}
