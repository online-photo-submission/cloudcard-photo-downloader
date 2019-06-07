package com.cloudcard.photoDownloader;

import java.sql.Timestamp;

public class NorthwesternPersonRecord {

    private String firstName;
    private String lastName;
    private String identifier;
    private Timestamp photoUpdated;
    private Timestamp expirationDate;

    private boolean isCardPhoto = false;

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

    public boolean needsCardPhoto() {
        return isCardPhoto;
    }

    public void setCardPhoto(boolean cardPhoto) {
        this.isCardPhoto = cardPhoto;
    }

    public Timestamp getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Timestamp expirationDate) {
        this.expirationDate = expirationDate;
    }
}

