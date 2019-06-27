package com.cloudcard.photoDownloader;

public class TransactCustomerPhoto {

    Long custId;
    byte[] photo;
//    NOTE: Remember to limit photo to 16777216 bytes elsewhere in application (?)


    public Long getCustId() {
        return custId;
    }

    public void setCustId(Long custId) {
        this.custId = custId;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }
}
