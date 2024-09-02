package com.cloudcard.photoDownloader


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SimpleResponseLogger {
    private static final Logger log = LoggerFactory.getLogger(SimpleResponseLogger.class)

    String source = this.class.simpleName
    // used to inform logger of error source. Put "simpleResponseLogger.source = this.class.simpleName" in init() method

    String log(String methodName, ResponseWrapper response, String customErrorMessage = "") {
        String standardResponseString = "$source - $methodName() Response status: $response.status"
        String message

        if (response.success) {
            message = "$standardResponseString success"
            log.info(message)
        } else {
            message = "$standardResponseString, ${customErrorMessage ?: response.body}"

            log.error(message)
        }

        return message
    }
}
