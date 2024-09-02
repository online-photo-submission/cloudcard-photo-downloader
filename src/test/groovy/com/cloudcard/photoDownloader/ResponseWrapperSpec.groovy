package com.cloudcard.photoDownloader

import com.mashape.unirest.http.HttpResponse
import com.mashape.unirest.http.Unirest
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

class ResponseWrapperSpec extends Specification {
    def "parseBody should should output 'No response body' without argument"() {
        given:
        Object body = ResponseWrapper.parseBody()

        expect:
        body.toString() == "No response body."
    }

    def "parseBody should should output correct object"() {
        given:
        Object body = ResponseWrapper.parseBody('{"Hello" : "World", "num" : 123 }')

        expect:
        body.Hello == "World"
        body.num == 123
    }

    def "parseBody should should output string if Object is in invalid format"() {
        given:
        Object body = ResponseWrapper.parseBody('["Hello" "World" }')

        expect:
        body == '["Hello" "World" }'
    }

    @Unroll
    def "isSuccessful should return true with 200-level status code - testing #code"() {
        when:
        boolean success = ResponseWrapper.isSuccessful(code)

        then:
        success == result

        where:
        code | result
        150 | false
        200 | true
        201 | true
        300 | false
        400 | false
        404 | false
        500 | false
    }

    def "constructor with Exception should work properly"() {
        when:
        ResponseWrapper response = new ResponseWrapper(new Exception("failed"))

        then:
        !response.success
        response.body == "failed"
        response.status == 500
        response.exception != null
    }

    def "constructor with AuthException should work properly"() {
        when:
        ResponseWrapper response = new ResponseWrapper(new AuthException("failed"))

        then:
        !response.success
        response.body == "failed Error while attempting to authenticate"
        response.status == 401
        response.exception != null
    }

    @Ignore
    def "constructor with HttpResponse<String> should work properly"() {
        given:
        HttpResponse<String> httpResponse = Unirest.get("https://www.google.com").asString()

        when:
        ResponseWrapper response = new ResponseWrapper(httpResponse)

        then:
        response.success
        response.status == 200
    }

    @Unroll
    def "custom constructor with should work properly"() {
        when:
        ResponseWrapper response = new ResponseWrapper(code, body)

        then:
        response.success == success
        response.status == code
        response.body == body

        where:
        code | body | success
        200 | "success" | true
        201 | "created" | true
        300 | "error" | false
        400 | "BADREQUEST" | false
        401 | "Unauthorized" | false
        404 | "Not Found" | false
        500 | "Server Error" | false
    }
}
