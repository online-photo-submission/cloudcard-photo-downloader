package com.cloudcard.photoDownloader

import spock.lang.Ignore
import spock.lang.Specification

class HttpClientSpec extends Specification {

    def "should be initialized"() {
        given:
        HttpClient httpClient = new HttpClient()

        expect:
        httpClient != null
    }

    def "makeRequest should error if called without required args"() {
        given:
        HttpClient httpClient = new HttpClient()

        when:
        httpClient.makeRequest()

        then:
        thrown(Exception)
    }

    def "makeRequest should give proper error if called with invalid action type"() {
        given:
        HttpClient httpClient = new HttpClient()

        when:
        def request = httpClient.makeRequest("ddelete", "url", ['foo': 'bar'])

        then:
        request.status == 400
        request.body == "Invalid Http action type. Aborted."
    }

    def "makeRequest should not allow string and file as bodies"() {
        given:
        HttpClient httpClient = new HttpClient()

        when:
        byte[] bytes = new byte[]{1}
        def request = httpClient.makeRequest("post", "url", ['foo': 'bar'], "string body", bytes)

        then:
        request.status == 400
        request.body == "Cannot send string and file in same request. Aborted."
    }

    @Ignore
    def "response should fail for invalid url"() {
        given:
        HttpClient httpClient = new HttpClient()

        when:
        String action = "GET"
        String url = "htttps://www.google.com"
        Map headers = [
                "Accept": "application/json" as String
        ]

        ResponseWrapper response = httpClient.makeRequest(action, url, headers)

        then:
        !response.success
    }

    def "makeRequest shouldn't require headers"() {
        given:
        HttpClient httpClient = new HttpClient()

        when:
        String action = "GET"
        String url = "https://www.google.com"

        ResponseWrapper response = httpClient.makeRequest(action, url)

        then:
        response.success
    }

    def "handleResponseLogging should log success message without body"() {
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
