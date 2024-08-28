package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class HttpClientSpec extends Specification {

    def "should be initialized"() {
        given:
        HttpClient httpClient = new HttpClient()

        expect:
        httpClient != null
    }

    def "source should be defined"() {
        given:
        HttpClient httpClient = new HttpClient()

        expect:
        httpClient.source != ""
        httpClient.source != null
        httpClient.source == "HttpClient"

    }

    def "handleResponseLogging should log success message without response body"() {
        given:
        HttpClient httpClient = new HttpClient()

        ResponseWrapper response = new ResponseWrapper(200, "OK")

        when:
        String message = httpClient.handleResponseLogging("testMethod", response)

        then:
        message == "HttpClient - testMethod() Response status: 200 success"
    }

    def "handleResponseLogging should log failed request with no custom message."() {
        given:
        HttpClient httpClient = new HttpClient()

        ResponseWrapper response = new ResponseWrapper(400, "Bad Request")

        when:
        String message = httpClient.handleResponseLogging("testMethod", response)

        then:
        message == "HttpClient - testMethod() Response status: 400, Bad Request"
    }

    def "handleResponseLogging should log failed request with custom message."() {
        given:
        HttpClient httpClient = new HttpClient()

        ResponseWrapper response = new ResponseWrapper(400, "This should not be logged!")

        when:
        String message = httpClient.handleResponseLogging("testMethod", response, "This is a custom message!")

        then:
        message == "HttpClient - testMethod() Response status: 400, This is a custom message!"
    }

}
