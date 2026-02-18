package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
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

    @Value("${downloader.cardholderGroupSubdirectory:false}")
    protected Boolean useCardholderGroupSubdirectories;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private FileNameResolver fileNameResolver;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private PostProcessor postProcessor;

    @Autowired
    FileService fileService;

    @PostConstruct
    void init() {

        throwIfTrue(fileNameResolver == null, "The File Name Resolver must be specified.");
        throwIfTrue(photoDirectories == null || photoDirectories.length == 0, "The Photo Directory(ies) must be specified.");

        log.info("      File Name Resolver : " + fileNameResolver.getClass().getSimpleName());
        log.info("    Photo Directory(ies) : " + String.join(" , ", photoDirectories));
        log.info("          Post-Processor : " + postProcessor.getClass().getSimpleName());
        log.info(" CardholderGroup Subdirs : " + useCardholderGroupSubdirectories);
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

        String baseName = fileNameResolver.getBaseName(photo);
        String directory = getDirectory(photo, photoDirectory);

        if (baseName == null || baseName.isEmpty()) {
            log.error("We could not resolve the base file name for '" + photo.getPerson().getEmail() + "' with ID number '"
                + photo.getPerson().getIdentifier() + "', so photo " + photo.getId() + " cannot be saved.");
            return null;
        }

        if (photo.getBytes() == null) {
            log.error("Photo " + photo.getId() + " for " + photo.getPerson().getEmail() + " is missing binary data, so it cannot be saved.");
            return null;
        }

        String fullFileName = fileService.writeBytesToFile(directory, baseName + ".jpg", photo.getBytes());

        PhotoFile photoFile = postProcessor.process(photo, directory, new PhotoFile(baseName, fullFileName, photo.getId()));

        return photoFile;
    }

    protected String getDirectory(Photo photo, String photoDirectory) {
        if (!useCardholderGroupSubdirectories) {
            return photoDirectory;
        }

        CardholderGroup cardholderGroup = photo.getPerson().getCardholderGroup();
        if (cardholderGroup == null || cardholderGroup.getName() == null) {
            log.warn("Photo " + photo.getId() + " has no cardholder group, using default directory");
            return photoDirectory;
        }

        try {
            String sanitizedName = sanitizeDirectoryName(cardholderGroup.getName());
            return photoDirectory + File.separator + sanitizedName;
        } catch (IllegalArgumentException e) {
            return photoDirectory;
        }
    }

    private String sanitizeDirectoryName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Directory name cannot be null or empty");
        }

        String original = name;
        // Remove path traversal sequences and path separators
        String sanitized = name
                .replaceAll("\\.\\.", "_")  // Replace .. with _
                .replaceAll("[/\\\\]", "_")  // Replace / and \ with _
                .replaceAll("[\\x00-\\x1F\\x7F]", "_")  // Replace control characters with _
                .trim();

        // Check for empty result after sanitization
        if (sanitized.isEmpty()) {
            throw new IllegalArgumentException("Directory name '" + original + "' is invalid after sanitization. Using default directory.");
        }

        // Log if the name was modified for security monitoring
        if (!sanitized.equals(original)) {
            log.warn("Cardholder group name sanitized for security: '" + original + "' -> '" + sanitized + "'. Using default directory.");
        }

        return sanitized;
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
