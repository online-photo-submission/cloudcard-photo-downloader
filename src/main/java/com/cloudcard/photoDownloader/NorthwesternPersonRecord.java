package com.cloudcard.photoDownloader;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public class NorthwesternPersonRecord {

    private String firstName;
    private String lastName;
    private String IdNumber;
    private String prefixedIdNumber;
    private Timestamp photoUpdated;
    private Timestamp expirationDate;

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

    public String getPrefixedIdNumber() {

        return prefixedIdNumber;
    }

    public void setPrefixedIdNumber(String prefixedIdNumber) {

        this.prefixedIdNumber = prefixedIdNumber;
    }

    public String getIdNumber() {

        return IdNumber;
    }

    public void setIdNumber(String idNumber) {

        IdNumber = idNumber;
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

        if (expirationDate == null) return true;
        LocalDateTime expirationDate = this.expirationDate.toLocalDateTime();
        LocalDateTime sixtyOneDaysFromNow = LocalDateTime.now().plusDays(61);
        return expirationDate.isBefore(sixtyOneDaysFromNow);
    }

    public Timestamp getExpirationDate() {

        return expirationDate;
    }

    public void setExpirationDate(Timestamp expirationDate) {

        this.expirationDate = expirationDate;
    }
}

