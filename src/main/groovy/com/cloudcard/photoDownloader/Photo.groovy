package com.cloudcard.photoDownloader

import com.fasterxml.jackson.annotation.*
import groovy.json.JsonSlurper
import groovy.transform.EqualsAndHashCode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import software.amazon.awssdk.services.sqs.model.Message

import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import static groovy.json.JsonOutput.prettyPrint
import static groovy.json.JsonOutput.toJson

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
    String dateCreatedString
    Date dateCreated
    Integer version
    Boolean backgroundReplaced
    Boolean helperBotReviewed

    Photo() {}

    Photo(int id) {
        this.id = id
    }

    static Photo fromSqsMessage(Message message) {
        Map map = new JsonSlurper().parseText(message.body()) as Map
        map.person = new Person(map.person as Map)
        map.links = new Links(map.links as Map)
        map.dateCreatedString = map.dateCreated
        map.dateCreated = parseDate(map.dateCreatedString)
        try {
            return new Photo(map)
        } catch (Exception e) {
            log.error("Error deserializing message ${message?.receiptHandle()}")
            log.error(e.message)
            log.error("message body follows:\n${prettyPrint(message.body())}")
            log.error("mapped values follow:\n${prettyPrint(toJson(map))}")
            return null
        }
    }

    static Date parseDate(def dateString) {
        try {
            String dateStringWithoutOffset = (dateString as String).take(19)
            LocalDateTime localDateTime = LocalDateTime.parse(dateStringWithoutOffset, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            return Timestamp.valueOf(localDateTime)
        } catch (Exception e) {
            log.error(e.message)
        }

        return null
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

    @JsonAnyGetter
    Map<String, Object> getAdditionalProperties() {
        return this.additionalProperties
    }

    @JsonAnySetter
    void setAdditionalProperty(String name, Object value) {
        this.additionalProperties.put(name, value)
    }
}
