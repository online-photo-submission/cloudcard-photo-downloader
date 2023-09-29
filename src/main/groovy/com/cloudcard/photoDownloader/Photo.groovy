package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"aspectRatio", "classifications", "domainClass", "id", "isAspectRatioCorrect", "links", "lowestClassification", "originalPhoto", "person", "publicKey", "status"})
public class Photo {

    private static final Logger log = LoggerFactory.getLogger(Photo.class);

    @JsonProperty("aspectRatio")
    private Double aspectRatio;
    @JsonProperty("classifications")
    private List<Object> classifications = null;
    @JsonProperty("domainClass")
    private String domainClass;
    @JsonProperty("id")
    private Integer id;
    @JsonProperty("isAspectRatioCorrect")
    private Boolean isAspectRatioCorrect;
    @JsonProperty("links")
    private Links links;
    @JsonProperty("lowestClassification")
    private Object lowestClassification;
    @JsonProperty("originalPhoto")
    private Object originalPhoto;
    @JsonProperty("person")
    private Person person;
    @JsonProperty("publicKey")
    private String publicKey;
    @JsonProperty("externalURL")
    private String externalURL;
    @JsonProperty("status")
    private String status;
    private byte[] bytes;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    public Photo() {

    }

    public Photo(Integer id) {

        this.id = id;
    }

    public Photo(Message message) {
        String messageBody = message.body();
        System.out.println("vvv");
        System.out.println(message);
        System.out.println(messageBody);
        System.out.println("^^^");
    }

    @JsonProperty("aspectRatio")
    public Double getAspectRatio() {

        return aspectRatio;
    }

    @JsonProperty("aspectRatio")
    public void setAspectRatio(Double aspectRatio) {

        this.aspectRatio = aspectRatio;
    }

    @JsonProperty("classifications")
    public List<Object> getClassifications() {

        return classifications;
    }

    @JsonProperty("classifications")
    public void setClassifications(List<Object> classifications) {

        this.classifications = classifications;
    }

    @JsonProperty("domainClass")
    public String getDomainClass() {

        return domainClass;
    }

    @JsonProperty("domainClass")
    public void setDomainClass(String domainClass) {

        this.domainClass = domainClass;
    }

    @JsonProperty("id")
    public Integer getId() {

        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {

        this.id = id;
    }

    @JsonProperty("isAspectRatioCorrect")
    public Boolean getIsAspectRatioCorrect() {

        return isAspectRatioCorrect;
    }

    @JsonProperty("isAspectRatioCorrect")
    public void setIsAspectRatioCorrect(Boolean isAspectRatioCorrect) {

        this.isAspectRatioCorrect = isAspectRatioCorrect;
    }

    @JsonProperty("links")
    public Links getLinks() {

        return links;
    }

    @JsonProperty("links")
    public void setLinks(Links links) {

        this.links = links;
    }

    @JsonProperty("lowestClassification")
    public Object getLowestClassification() {

        return lowestClassification;
    }

    @JsonProperty("lowestClassification")
    public void setLowestClassification(Object lowestClassification) {

        this.lowestClassification = lowestClassification;
    }

    @JsonProperty("originalPhoto")
    public Object getOriginalPhoto() {

        return originalPhoto;
    }

    @JsonProperty("originalPhoto")
    public void setOriginalPhoto(Object originalPhoto) {

        this.originalPhoto = originalPhoto;
    }

    @JsonProperty("person")
    public Person getPerson() {

        return person;
    }

    @JsonProperty("person")
    public void setPerson(Person person) {

        this.person = person;
    }

    @JsonProperty("publicKey")
    public String getPublicKey() {

        return publicKey;
    }

    @JsonProperty("publicKey")
    public void setPublicKey(String publicKey) {

        this.publicKey = publicKey;
    }

    @JsonProperty("status")
    public String getStatus() {

        return status;
    }

    @JsonProperty("status")
    public void setStatus(String status) {

        this.status = status;
    }

    public String getExternalURL() {

        return externalURL;
    }

    @JsonProperty("externalURL")
    public void setExternalURL(String externalURL) {

        this.externalURL = externalURL;
    }

    public byte[] getBytes() {

        return bytes;
    }

    public void setBytes(byte[] bytes) {

        this.bytes = bytes;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {

        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {

        this.additionalProperties.put(name, value);
    }
}
