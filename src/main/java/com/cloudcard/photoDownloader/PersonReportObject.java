package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({"email", "identifier", "employeeNumber"})
public class PersonReportObject {

    @JsonProperty("email")
    String email;
    @JsonProperty("identifier")
    String identifier;
    @JsonProperty("Employee_Number")
    String employeeNumber;

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

    @JsonProperty("Employee_Number")
    public String getEmployeeNumber() {
        return employeeNumber;
    }

    @JsonProperty("Employee_Number")
    public void setEmployeeNumber(String employeeNumber) {
        this.employeeNumber = employeeNumber;
    }
}
