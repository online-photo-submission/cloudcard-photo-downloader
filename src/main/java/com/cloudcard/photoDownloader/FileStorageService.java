package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FileStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${downloader.photoDirectory}")
    private String photoDirectory;

    @Value("${downloader.minPhotoIdLength}")
    private Integer minPhotoIdLength;

    public FileStorageService() {

    }

    public FileStorageService(String photoDirectory, Integer minPhotoIdLength) {

        this.photoDirectory = photoDirectory;
        this.minPhotoIdLength = minPhotoIdLength;
    }

    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {

        List<PhotoFile> photoFiles = new ArrayList<>();
        for (Photo photo : photos) {
            log.info("Saving: " + photo.getPublicKey());
            PhotoFile photoFile = save(photo);
            if (photoFile != null) photoFiles.add(photoFile);
        }

        return photoFiles;
    }

    protected PhotoFile save(Photo photo) throws Exception {

        String studentID = getStudentID(photo);

        if (studentID == null || studentID.isEmpty()) {
            log.error(photo.getPerson().getEmail() + " is missing an ID number, so photo " + photo.getId() + " cannot be saved.");
            return null;
        }

        if (photo.getBytes() == null) {
            log.error("Photo " + photo.getId() + " for " + photo.getPerson().getEmail() + " is missing binary data, so it cannot be saved.");
            return null;
        }

        String fileName = writeBytesToFile(photoDirectory, studentID + ".jpg", photo.getBytes());

        return new PhotoFile(studentID, fileName, photo.getId());
    }

    protected String getStudentID(Photo photo) {

        String identifier = photo.getPerson().getIdentifier();
        if (identifier == null || identifier.isEmpty() || minPhotoIdLength < identifier.length()) return identifier;
        return String.format("%" + minPhotoIdLength + "s", identifier).replace(' ', '0');
    }

    private String writeBytesToFile(String directoryName, String fileName, byte[] bytes) throws IOException {

        File directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directoryName + File.separator + fileName);

        FileOutputStream outputStream = new FileOutputStream(file);

        if (!file.exists()) {
            file.createNewFile();
        }
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();

        return file.getCanonicalPath();
    }

}
