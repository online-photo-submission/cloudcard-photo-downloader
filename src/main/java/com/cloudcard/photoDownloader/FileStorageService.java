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
    String photoDirectory;

    @Value("${downloader.slash}")
    private String slash;

    @Value("${downloader.enableUdf}")
    private boolean enableUdf;

    @Value("${downloader.udfDirectory}")
    String udfDirectory;

    @Value("${downloader.udfFilePrefix}")
    private String udfFilePrefix;

    @Value("${downloader.udfFileExtension}")
    private String udfFileExtension;

    @Value("${downloader.descriptionDateFormat}")
    private String descriptionDateFormat;

    @Value("${downloader.batchIdDateFormat}")
    private String batchIdDateFormat;

    @Value("${downloader.createdDateFormat}")
    private String createdDateFormat;

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

        String studentID = photo.getPerson().getIdentifier();

        if (studentID == null || studentID.isEmpty()) {
            log.error(photo.getPerson().getEmail() + " is missing an ID number, so photo " + photo.getId() + " cannot be saved.");
            return null;
        }

        if (photo.getBytes() == null) {
            log.error("Photo " + photo.getId() + " for " + photo.getPerson().getEmail() + " is missing binary data, so it cannot be saved.");
            return null;
        }

        String fileName = photoDirectory + slash + studentID + ".jpg";
        writeBytesToFile(fileName, photo.getBytes());
        return new PhotoFile(studentID, fileName);
    }

    private void writeBytesToFile(String fileName, byte[] bytes) throws IOException {

        File file = new File(fileName);

        FileOutputStream outputStream = new FileOutputStream(file);

        if (!file.exists()) {
            file.createNewFile();
        }
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }

}
