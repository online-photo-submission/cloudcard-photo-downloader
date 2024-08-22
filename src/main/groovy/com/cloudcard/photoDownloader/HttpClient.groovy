package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import groovy.json.JsonException
import groovy.json.JsonSlurper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class HttpClient {
    // This class can instantiated for easy HTTP requests to a given API through the makeRequests() method. If you need to send custom requests with multiple body types such as json strings and files, it is recommended to build that request separately.

    private static final Logger log = LoggerFactory.getLogger(HttpClient.class)

    String source = "HttpClient"
    // used to inform logger of error source. Put "httpClient.source = this.class.name" in init() method

    ResponseWrapper makeRequest(String actionType, String url, Map headers = null, String bodyString = "", byte[] bodyBytes = null) {
        // Provides an all-in-one http request builder which packages unirest client into a single method call

        if (!Actions.values().any {it.value == actionType.toUpperCase()}) {
            return new ResponseWrapper(400, "Invalid Http action type. Aborted.")
        }
        if (bodyString && bodyBytes) {
            return new ResponseWrapper(400, "Cannot send string and file in same request. Aborted.")
        }

        Body body
        if (bodyString) body = new Body(bodyString)
        else if (bodyBytes) body = new Body(bodyBytes)
        else body = null

        Closure request = configureRequest(actionType, url, headers, body)
        ResponseWrapper wrapper

        try {
            wrapper = new ResponseWrapper(request())
        } catch (Exception e) { // ?
            log.error(e.message)
            wrapper = new ResponseWrapper(e)
        }

        return wrapper
    }

    private Closure configureRequest(String actionType, String url, Map headers = null, Body body = null) {

        HttpResponse<String> response
        Closure request = {
            response = Unirest.get(url)
                    .headers(headers)
                    .asString()
        }

        switch (actionType) {
            case Actions.POST.value:
                request = {
                    response = Unirest.post(url)
                            .headers(headers)
                            .body(body.data)
                            .asString()
                }
                break
            case Actions.PUT.value:
                request = {
                    response = Unirest.put(url)
                            .headers(headers)
                            .body(body.data)
                            .asString()
                }
                break
            case Actions.PATCH.value:
                request = {
                    response = Unirest.patch(url)
                            .headers(headers)
                            .body(body.data)
                            .asString()
                }
                break
            case Actions.DELETE.value:
                request = {
                    response = Unirest.delete(url)
                            .headers(headers)
                            .asString()
                }
                break
            case Actions.GET.value: // get is set as default
                break
        }

        return request
    }

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

enum Actions {
    GET("GET"), POST("POST"), PUT("PUT"), PATCH("PATCH"), DELETE("DELETE")

    String value

    Actions(String action) {
        this.value = action
    }
}

class ResponseWrapper {
    Exception exception
    boolean success
    Object body
    int status

    static Object parseBody(String body = null) {
        Object result

        if (body) {
            try {
                result = new JsonSlurper().parseText(body)
            } catch (JsonException ignored) {
                result = body.toString()
            }
        } else {
            result = "No response body."
        }

        return result
    }

    static boolean isSuccessful(int code) {
        return code >= 200 && code < 300
    }

    ResponseWrapper(int code, String responseBody = "No response body") {
        body = parseBody(responseBody)
        status = code as int
        success = isSuccessful(code)
    }

    ResponseWrapper(HttpResponse<String> response) {
        body = parseBody(response?.body)
        status = response.status as int
        success = isSuccessful(response.status)
    }

    ResponseWrapper(Exception e) {
        exception = e
        status = 500
        success = false
        body = parseBody(e.getMessage())
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
