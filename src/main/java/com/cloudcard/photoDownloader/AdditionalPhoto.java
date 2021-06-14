package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.annotation.*;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AdditionalPhoto {

    private static final Logger log = LoggerFactory.getLogger(AdditionalPhoto.class);

    @JsonProperty("id")
    private Integer id;
    private String typeName;
    private String externalURL;
    @JsonProperty("publicKey")
    private String publicKey;
    @JsonIgnore
    private Map<String, Object> additionalProperties = new HashMap<String, Object>();
    private byte[] bytes;


    @JsonProperty("id")
    public Integer getId() {

        return id;
    }

    @JsonProperty("id")
    public void setId(Integer id) {

        this.id = id;
    }

    public String getTypeName() {
        return typeName;
    }

    @JsonProperty("additionalPhotoType")
    public void setAdditionalPhotoType(AdditionalPhotoType additionalPhotoType) {
        this.typeName = additionalPhotoType.getName();
    }

    @JsonProperty("links")
    public void setLinks(Links links) {

        this.externalURL = links.getBytes();
    }

    public String getExternalURL() {
        return externalURL;
    }

    public void setExternalURL(String externalURL) {
        this.externalURL = externalURL;
    }

    @JsonProperty("publicKey")
    public String getPublicKey() {

        return publicKey;
    }

    @JsonProperty("publicKey")
    public void setPublicKey(String publicKey) {

        this.publicKey = publicKey;
    }

    @JsonAnyGetter
    public Map<String, Object> getAdditionalProperties() {

        return this.additionalProperties;
    }

    @JsonAnySetter
    public void setAdditionalProperty(String name, Object value) {

        this.additionalProperties.put(name, value);
    }

    public byte[] getBytes() {

        if (bytes == null) {
            try {
                bytes = RestUtil.fetchBytes(externalURL);
            } catch (Exception e) {
                log.error(e.getMessage());
                e.printStackTrace();
            }
        }
        return bytes;
    }
}
