package com.cloudcard.photoDownloader;

import java.util.Objects;

public class PhotoFile {

    private String studentId;
    private String fileName;
    private Integer photoId;

    public PhotoFile(String studentId, String fileName, Integer photoId) {

        this.studentId = studentId;
        this.fileName = fileName;
        this.photoId = photoId;
    }

    public String getStudentId() {

        return studentId;
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
        return Objects.equals(studentId, photoFile.studentId) && Objects.equals(fileName, photoFile.fileName);
    }

    @Override
    public int hashCode() {

        return Objects.hash(studentId, fileName);
    }

    @Override
    public String toString() {

        return "PhotoFile{" + "studentId='" + studentId + '\'' + ", fileName='" + fileName + '\'' + ", photoId=" + photoId + '}';
    }
}
