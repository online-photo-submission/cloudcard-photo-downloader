package com.cloudcard.photoDownloader

import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder

import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping

@RestController
@RequestMapping('/api')
class OrigoController {

    @PostMapping('/cloudevent')
    CloudEvent handleCloudEvent(@RequestBody Map<String, Object> requestBody) {
        String eventId = requestBody.get("id")
        String source = requestBody.get("source")
        String type = requestBody.get("type")
        String data = requestBody.get("data")

        CloudEvent event = CloudEventBuilder.v1()
                .withId(eventId)
                .withSource(URI.create(source))
                .withType(type)
                .withTime(OffsetDateTime.now())
                .withData(data.getBytes())
                .build()

        return event

    }
}