package com.cloudcard.photoDownloader

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.transform.EqualsAndHashCode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.model.Message

import static groovy.json.JsonOutput.prettyPrint

@EqualsAndHashCode(includes = ["publicKey"])
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder(["aspectRatio", "classifications", "domainClass", "id", "isAspectRatioCorrect", "links", "lowestClassification", "originalPhoto", "person", "publicKey", "status"])
class Photo {

    static final Logger log = LoggerFactory.getLogger(Photo.class)

    @JsonProperty("aspectRatio")
    Double aspectRatio
    @JsonProperty("classifications")
    List<Object> classifications = null
    @JsonProperty("domainClass")
    String domainClass
    @JsonProperty("id")
    Integer id
    @JsonProperty("isAspectRatioCorrect")
    Boolean isAspectRatioCorrect
    @JsonProperty("links")
    Links links
    @JsonProperty("lowestClassification")
    Object lowestClassification
    @JsonProperty("originalPhoto")
    Object originalPhoto
    @JsonProperty("person")
    Person person
    @JsonProperty("publicKey")
    String publicKey
    @JsonProperty("externalURL")
    String externalURL
    @JsonProperty("status")
    String status
    byte[] bytes
    @JsonIgnore
    Map<String, Object> additionalProperties = new HashMap<String, Object>()
    Boolean manuallyEdited
    Date dateCreated
    Integer version
    Boolean backgroundReplaced
    Boolean helperBotReviewed

    Photo() {}

    Photo(Integer id) {
        this.id = id
    }

    static Photo fromSqsMessage(Message message) {
        try {
            return new ObjectMapper().readValue(message.body(), new TypeReference<Photo>() { }) as Photo
        } catch (Exception e) {
            log.error("Error deserializing message ${message?.receiptHandle()}")
            log.error(e.message)
            log.error("message body follows:\n${prettyPrint(message.body())}")
            return null
        }
    }

    @JsonProperty("aspectRatio")
    Double getAspectRatio() {
        return aspectRatio
    }

    @JsonProperty("aspectRatio")
    void setAspectRatio(Double aspectRatio) {
        this.aspectRatio = aspectRatio
    }

    @JsonProperty("classifications")
    List<Object> getClassifications() {
        return classifications
    }

    @JsonProperty("classifications")
    void setClassifications(List<Object> classifications) {
        this.classifications = classifications
    }

    @JsonProperty("domainClass")
    String getDomainClass() {
        return domainClass
    }

    @JsonProperty("domainClass")
    void setDomainClass(String domainClass) {
        this.domainClass = domainClass
    }

    @JsonProperty("id")
    Integer getId() {
        return id
    }

    @JsonProperty("id")
    void setId(Integer id) {
        this.id = id
    }

    @JsonProperty("isAspectRatioCorrect")
    Boolean getIsAspectRatioCorrect() {
        return isAspectRatioCorrect
    }

    @JsonProperty("isAspectRatioCorrect")
    void setIsAspectRatioCorrect(Boolean isAspectRatioCorrect) {
        this.isAspectRatioCorrect = isAspectRatioCorrect
    }

    @JsonProperty("links")
    Links getLinks() {
        return links
    }

    @JsonProperty("links")
    void setLinks(Links links) {
        this.links = links
    }

    @JsonProperty("lowestClassification")
    Object getLowestClassification() {
        return lowestClassification
    }

    @JsonProperty("lowestClassification")
    void setLowestClassification(Object lowestClassification) {
        this.lowestClassification = lowestClassification
    }

    @JsonProperty("originalPhoto")
    Object getOriginalPhoto() {
        return originalPhoto
    }

    @JsonProperty("originalPhoto")
    void setOriginalPhoto(Object originalPhoto) {
        this.originalPhoto = originalPhoto
    }

    @JsonProperty("person")
    Person getPerson() {
        return person
    }

    @JsonProperty("person")
    void setPerson(Person person) {
        this.person = person
    }

    @JsonProperty("publicKey")
    String getPublicKey() {
        return publicKey
    }

    @JsonProperty("publicKey")
    void setPublicKey(String publicKey) {
        this.publicKey = publicKey
    }

    @JsonProperty("status")
    String getStatus() {
        return status
    }

    @JsonProperty("status")
    void setStatus(String status) {
        this.status = status
    }

    String getExternalURL() {
        return externalURL ?: links?.bytes
    }

    @JsonProperty("externalURL")
    void setExternalURL(String externalURL) {
        this.externalURL = externalURL
    }

    byte[] getBytes() {
        return bytes
    }

    void setBytes(byte[] bytes) {
        this.bytes = bytes
    }

    String getBytesBase64() {
        if (!this.bytes) {
            log.error("Photo $this.id for $this.person.email is missing binary data, so it cannot be downloaded.")
            return null
        }

        return Base64.getEncoder().encodeToString(this.bytes)
    }

    @JsonAnyGetter
    Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties
    }

    @JsonAnySetter
    void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value)
    }
}
