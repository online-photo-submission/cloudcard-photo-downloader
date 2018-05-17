package com.cloudcard.photoDownloader;

import java.util.Objects;

public class PhotoFile {

    private String idNumber;
    private String fileName;

    public PhotoFile(String idNumber, String fileName) {

        this.idNumber = idNumber;
        this.fileName = fileName;
    }

    public String getIdNumber() {

        return idNumber;
    }

    public void setIdNumber(String idNumber) {

        this.idNumber = idNumber;
    }

    public String getFileName() {

        return fileName;
    }

    public void setFileName(String fileName) {

        this.fileName = fileName;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoFile photoFile = (PhotoFile) o;
        return Objects.equals(idNumber, photoFile.idNumber) && Objects.equals(fileName, photoFile.fileName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(idNumber, fileName);
    }

    @Override
    public String toString() {

        return "PhotoFile{" + "idNumber='" + idNumber + '\'' + ", fileName='" + fileName + '\'' + '}';
    }
}
