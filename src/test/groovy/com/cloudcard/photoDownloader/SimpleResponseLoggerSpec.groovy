package com.cloudcard.photoDownloader


import spock.lang.Specification

class SimpleResponseLoggerSpec extends Specification {

    def "should be initialized"() {
        given:
        SimpleResponseLogger simpleResponseLogger = new SimpleResponseLogger()

        expect:
        simpleResponseLogger != null
    }

    def "source should be defined"() {
        given:
        SimpleResponseLogger simpleResponseLogger = new SimpleResponseLogger()

        expect:
        simpleResponseLogger.source != ""
        simpleResponseLogger.source != null
        simpleResponseLogger.source == "SimpleResponseLogger"

    }

    def "log should log success message without response body"() {
        given:
        SimpleResponseLogger simpleResponseLogger = new SimpleResponseLogger()

        ResponseWrapper response = new ResponseWrapper(200, "OK")

        when:
        String message = simpleResponseLogger.log("testMethod", response)

        then:
        message == "SimpleResponseLogger - testMethod() Response status: 200 success"
    }

    def "log should log failed request with no custom message."() {
        given:
        SimpleResponseLogger simpleResponseLogger = new SimpleResponseLogger()

        ResponseWrapper response = new ResponseWrapper(400, "Bad Request")

        when:
        String message = simpleResponseLogger.log("testMethod", response)

        then:
        message == "SimpleResponseLogger - testMethod() Response status: 400, Bad Request"
    }

    def "log should log failed request with custom message."() {
        given:
        SimpleResponseLogger simpleResponseLogger = new SimpleResponseLogger()

        ResponseWrapper response = new ResponseWrapper(400, "This should not be logged!")

        when:
        String message = simpleResponseLogger.log("testMethod", response, "This is a custom message!")

        then:
        message == "SimpleResponseLogger - testMethod() Response status: 400, This is a custom message!"
    }

}
