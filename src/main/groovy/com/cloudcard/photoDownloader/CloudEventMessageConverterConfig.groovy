package com.cloudcard.photoDownloader

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import io.cloudevents.spring.messaging.CloudEventMessageConverter

@Configuration
class CloudEventMessageConverterConfiguration {

    @Bean
    CloudEventMessageConverter cloudEventMessageConverter() {
        return new CloudEventMessageConverter()
    }
}
