package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "FileStorageService")
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
            log.info("Saving: " + photo.getId() + " for person: " + photo.getPerson().getEmail());
            for (String photoDirectory : photoDirectories) {
                PhotoFile photoFile = save(photo, photoDirectory);
                if (photoFile != null) photoFiles.add(photoFile);
            }
        }

        return photoFiles;
    }

    protected PhotoFile save(Photo photo, String photoDirectory) throws Exception {

        String baseName = getBaseName(photo);

        if (baseName == null || baseName.isEmpty()) {
            log.error("We could not resolve the base file name for '" + photo.getPerson().getEmail() + "' with ID number '"
                + photo.getPerson().getIdentifier() + "', so photo " + photo.getId() + " cannot be saved.");
            return null;
        }

        if (photo.getBytes() == null) {
            log.error("Photo " + photo.getId() + " for " + photo.getPerson().getEmail() + " is missing binary data, so it cannot be saved.");
            return null;
        }

        String fullFileName = writeBytesToFile(photoDirectory, baseName + ".jpg", photo.getBytes());

        PhotoFile photoFile = postProcess(photo, photoDirectory, new PhotoFile(baseName, fullFileName, photo.getId()));

        return photoFile;
    }

    /**
     * This is a place holder for child classes to execute a post processing strategy
     *
     * @param photoFile
     */
    protected PhotoFile postProcess(Photo photo, String photoDirectory, PhotoFile photoFile) {
        // do nothing
        return photoFile;
    }

    protected String getBaseName(Photo photo) {

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
