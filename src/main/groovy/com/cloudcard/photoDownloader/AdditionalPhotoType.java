package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdditionalPhotoType {


    @JsonProperty("name")
    private String name;

    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();

    AdditionalPhotoType() {}

    AdditionalPhotoType(String name) {
        this.name = name;
    }


    @JsonProperty("name")
    public String getName() {

        return name;
    }

    @JsonProperty("name")
    public void setName(String name) {

        this.name = name;
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
