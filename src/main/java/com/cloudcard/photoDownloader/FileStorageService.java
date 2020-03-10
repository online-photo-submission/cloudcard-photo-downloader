package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue;

@Service
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "FileStorageService")
public class FileStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    @Value("${downloader.photoDirectories}")
    private String[] photoDirectories;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private FileNameResolver fileNameResolver;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    PreProcessor preProcessor;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private PostProcessor postProcessor;

    @PostConstruct
    void init() {

        throwIfTrue(fileNameResolver == null, "The File Name Resolver must be specified.");
        throwIfTrue(photoDirectories == null || photoDirectories.length == 0, "The Photo Directory(ies) must be specified.");

        log.info("   File Name Resolver : " + fileNameResolver.getClass().getSimpleName());
        log.info(" Photo Directory(ies) : " + String.join(" , ", photoDirectories));
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

        photo = preProcessor.process(photo);

        String baseName = fileNameResolver.getBaseName(photo);

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

        PhotoFile photoFile = postProcessor.process(photo, photoDirectory, new PhotoFile(baseName, fullFileName, photo.getId()));

        return photoFile;
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
