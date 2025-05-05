package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import groovy.json.JsonException
import groovy.json.JsonSlurper

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

    ResponseWrapper(AuthException e) {
        exception = e
        status = 401
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
