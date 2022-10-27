package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthenticationToken {


    @JsonProperty("tokenValue")
    private String tokenValue;

    @JsonProperty("id")
    private int id;

    @JsonProperty("dateCreated")
    private String dateCreated;

    @JsonProperty("lastUpdated")
    private String lastUpdated;

    @JsonProperty("username")
    private String username;

    @JsonProperty("domainClass")
    private String domainClass;

    @JsonProperty("version")
    private int version;

    @JsonProperty("expirationDate")
    private String expirationDate;

    @JsonProperty("name")
    public String getTokenValue() {

        return tokenValue;
    }

    @JsonProperty("name")
    public void setTokenValue(String tokenValue) {

        this.tokenValue = tokenValue;
    }

}
