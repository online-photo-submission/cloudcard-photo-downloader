package com.cloudcard.photoDownloader

import com.mashape.unirest.http.exceptions.UnirestException
import spock.lang.Ignore
import spock.lang.Specification

class HttpClientSpec extends Specification{

    def "should be initialized"() {
        HttpClient httpClient = new HttpClient()

        expect:
        httpClient != null
    }

    def "makeRequest should error if called without required args"() {
        HttpClient httpClient = new HttpClient()

        when:
        httpClient.makeRequest()

        then:
        thrown(Exception)
    }

    def "makeRequest should give proper error if called with invalid action type"() {
        HttpClient httpClient = new HttpClient()

        when:
        def request = httpClient.makeRequest("ddelete", "url", ['foo' : 'bar'])

        then:
        request.status == 400
        request.body == "Invalid Http action type. Aborted."
    }

    def "makeRequest should not allow string and file as bodies"() {
        HttpClient httpClient = new HttpClient()

        when:
        byte[] bytes = new byte[] {1}
        def request = httpClient.makeRequest("post", "url", ['foo' : 'bar'], "string body", bytes)

        then:
        request.status == 400
        request.body == "Cannot send string and file in same request. Aborted."
    }

//    @Ignore
    def "response should fail for invalid url"() {
        HttpClient httpClient = new HttpClient()

        when:
        String action = "GET"
        String url = "https://www.google.com"
        Map headers = [
                "Accept": "application/json" as String
        ]

        ResponseWrapper response = httpClient.makeRequest(action, url, headers)

        then:
        !response.success
    }


}
