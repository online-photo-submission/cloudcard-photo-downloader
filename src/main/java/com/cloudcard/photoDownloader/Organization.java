package com.cloudcard.photoDownloader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id",
        "name",
        "photoRequirements",
        "identifier",
        "badPhotoThreshold",
        "goodPhotoThreshold"
})
public class Organization {

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("name")
    private String name;
    @JsonProperty("photoRequirements")
    private List<Object> photoRequirements = null;
    @JsonProperty("identifier")
    private Object identifier;
    @JsonProperty("badPhotoThreshold")
    private Double badPhotoThreshold;
    @JsonProperty("goodPhotoThreshold")
    private Double goodPhotoThreshold;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    @JsonProperty("id")
    public Integer getId() {
        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {
        this.id = id;
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {
        this.name = name;
    }

    @JsonProperty("photoRequirements")
    public List<Object> getPhotoRequirements() {
        return photoRequirements;
    }

    @JsonProperty("photoRequirements")
    public void setPhotoRequirements(List<Object> photoRequirements) {
        this.photoRequirements = photoRequirements;
    }

    @JsonProperty("identifier")
    public Object getIdentifier() {
        return identifier;
    }

    @JsonProperty("identifier")
    public void setIdentifier(Object identifier) {
        this.identifier = identifier;
    }

    @JsonProperty("badPhotoThreshold")
    public Double getBadPhotoThreshold() {
        return badPhotoThreshold;
    }

    @JsonProperty("badPhotoThreshold")
    public void setBadPhotoThreshold(Double badPhotoThreshold) {
        this.badPhotoThreshold = badPhotoThreshold;
    }

    @JsonProperty("goodPhotoThreshold")
    public Double getGoodPhotoThreshold() {
        return goodPhotoThreshold;
    }

    @JsonProperty("goodPhotoThreshold")
    public void setGoodPhotoThreshold(Double goodPhotoThreshold) {
        this.goodPhotoThreshold = goodPhotoThreshold;
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