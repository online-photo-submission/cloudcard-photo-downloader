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

//@Service
public class FileStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${downloader.photoDirectories}")
    private String[] photoDirectories;

    @Value("${downloader.minPhotoIdLength}")
    private Integer minPhotoIdLength;

    public FileStorageService() {

    }

    public FileStorageService(String[] photoDirectories, Integer minPhotoIdLength) {

        this.photoDirectories = photoDirectories;
        this.minPhotoIdLength = minPhotoIdLength;
    }

    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {

        List<PhotoFile> photoFiles = new ArrayList<>();
        for (Photo photo : photos) {
            log.info("Saving: " + photo.getPublicKey());
            for (String photoDirectory : photoDirectories) {
                PhotoFile photoFile = save(photo, photoDirectory);
                if (photoFile != null) photoFiles.add(photoFile);
            }
        }

        return photoFiles;
    }

    protected PhotoFile save(Photo photo, String photoDirectory) throws Exception {

        String fileName = getFileName(photo);

        if (fileName == null || fileName.isEmpty()) {
            log.error(photo.getPerson().getEmail() + " is missing an ID number, so photo " + photo.getId() + " cannot be saved.");
            return null;
        }

        if (photo.getBytes() == null) {
            log.error("Photo " + photo.getId() + " for " + photo.getPerson().getEmail() + " is missing binary data, so it cannot be saved.");
            return null;
        }

        String fullFileName = writeBytesToFile(photoDirectory, fileName + ".jpg", photo.getBytes());

        return new PhotoFile(getStudentID(photo), fullFileName, photo.getId());
    }

    protected String getFileName(Photo photo) {

        return getStudentID(photo);
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
