package com.cloudcard.photoDownloader;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

public class NorthwesternPersonRecord {

    private String firstName;
    private String lastName;
    private String identifier;
    private Timestamp photoUpdated;

    private boolean needsUpdated = false;

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Timestamp getPhotoUpdated() {
        return photoUpdated;
    }

    public void setPhotoUpdated(Timestamp photoUpdated) {
        this.photoUpdated = photoUpdated;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String toString() {
        return "NorthwesternPersonRecord{" +
                "firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", photoUpdated=" + photoUpdated +
                '}';
    }

    public boolean needsUpdate() {
        return needsUpdated;
    }

    public void setNeedsUpdated(boolean needsUpdated) {
        this.needsUpdated = needsUpdated;
    }
}

