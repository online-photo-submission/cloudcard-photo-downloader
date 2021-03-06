package com.cloudcard.photoDownloader;

import java.util.Objects;

public class PhotoFile {

    private String baseName;
    private String fileName;
    private Integer photoId;

    public PhotoFile(String baseName, String fileName, Integer photoId) {

        this.baseName = baseName;
        this.fileName = fileName;
        this.photoId = photoId;
    }

    public String getBaseName() {

        return baseName;
    }

    public String getFileName() {

        return fileName;
    }

    public Integer getPhotoId() {

        return photoId;
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhotoFile photoFile = (PhotoFile) o;
        return Objects.equals(baseName, photoFile.baseName) && Objects.equals(fileName, photoFile.fileName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(baseName, fileName);
    }

    @Override
    public String toString() {

        return "PhotoFile{" + "baseName='" + baseName + '\'' + ", fileName='" + fileName + '\'' + ", photoId=" + photoId + '}';
    }
}
