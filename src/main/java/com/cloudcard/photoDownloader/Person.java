package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"id", "username", "email", "identifier", "organization", "additionalPhotos"})
public class Person {

    @JsonProperty("id")
    private Integer id;
    @JsonProperty("username")
    private String username;
    @JsonProperty("email")
    private String email;
    @JsonProperty("identifier")
    private String identifier;
    @JsonProperty("organization")
    private Organization organization;
    @JsonProperty("additionalPhotos")
    private List<Object> additionalPhotos = null;
    @JsonProperty("customFields")
    private Map<String, String> customFields;
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

    @JsonProperty("username")
    public String getUsername() {

        return username;
    }

    @JsonProperty("username")
    public void setUsername(String username) {

        this.username = username;
    }

    @JsonProperty("email")
    public String getEmail() {

        return email;
    }

    @JsonProperty("email")
    public void setEmail(String email) {

        this.email = email;
    }

    @JsonProperty("identifier")
    public String getIdentifier() {

        return identifier;
    }

    @JsonProperty("identifier")
    public void setIdentifier(String identifier) {

        this.identifier = identifier;
    }

    @JsonProperty("organization")
    public Organization getOrganization() {

        return organization;
    }

    @JsonProperty("organization")
    public void setOrganization(Organization organization) {

        this.organization = organization;
    }

    @JsonProperty("additionalPhotos")
    public List<Object> getAdditionalPhotos() {

        return additionalPhotos;
    }

    @JsonProperty("additionalPhotos")
    public void setAdditionalPhotos(List<Object> additionalPhotos) {

        this.additionalPhotos = additionalPhotos;
    }

    public Map<String, String> getCustomFields() {

        return customFields;
    }

    @JsonProperty("customFields")
    public void setCustomFields(Map<String, String> customFields) {

        this.customFields = customFields;
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