package com.cloudcard.photoDownloader


import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import groovy.transform.EqualsAndHashCode

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder([
        "domainClass", "publicKey", "manuallyEdited", "person", "status",
        "originalPhoto", "dateCreated", "id", "links", "version",
        "lowestClassification", "backgroundReplaced", "aspectRatio",
        "helperBotReviewed", "isAspectRatioCorrect"
])
@EqualsAndHashCode(includes = ["publicKey"])

class PhotoStatusMessage {

    @JsonProperty("domainClass")
    String domainClass

    @JsonProperty("publicKey")
    String publicKey

    @JsonProperty("manuallyEdited")
    Boolean manuallyEdited

    @JsonProperty("person")
    Person person

    @JsonProperty("status")
    String status

    @JsonProperty("originalPhoto")
    Object originalPhoto

    @JsonProperty("dateCreated")
    String dateCreated

    @JsonProperty("id")
    Integer id

    @JsonProperty("links")
    Links links

    @JsonProperty("version")
    Integer version

    @JsonProperty("lowestClassification")
    Double lowestClassification

    @JsonProperty("backgroundReplaced")
    Boolean backgroundReplaced

    @JsonProperty("aspectRatio")
    Double aspectRatio

    @JsonProperty("helperBotReviewed")
    Boolean helperBotReviewed

    @JsonProperty("isAspectRatioCorrect")
    Boolean isAspectRatioCorrect
}