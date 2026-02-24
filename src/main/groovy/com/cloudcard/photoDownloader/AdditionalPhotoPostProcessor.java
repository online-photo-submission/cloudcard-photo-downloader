package com.cloudcard.photoDownloader;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Service
@ConditionalOnProperty(value = "downloader.postProcessor", havingValue = "AdditionalPhotoPostProcessor")
public class AdditionalPhotoPostProcessor implements PostProcessor {

    private static final Logger log = LoggerFactory.getLogger(AdditionalPhotoPostProcessor.class);

    @Autowired
    FileService fileService;

    //TODO refactor this to the CloudCardClient.
    @Autowired
    RestService restService;

    @Value("${AdditionalPhotoPostProcessor.include}")
    String[] include;

    @Override
    public PhotoFile process(Photo photo, String photoDirectory, PhotoFile photoFile) {

        for (AdditionalPhoto additionalPhoto : photo.getPerson().getAdditionalPhotos()) {

            if(include != null && !Arrays.asList(include).contains(additionalPhoto.getTypeName())) {
                continue; //skip this one
            }

            String directoryName = photoDirectory + "/" + additionalPhoto.getTypeName();
            try {
                restService.fetchBytes(additionalPhoto);
                fileService.writeBytesToFile(directoryName, photoFile.getBaseName() + ".jpg", additionalPhoto.getBytes());
            } catch (Exception e) {
                log.error(e.getMessage());
                log.error("Failed to save additional photo (" + additionalPhoto.getTypeName() + ") for person " + photo.getPerson().getIdentifier());
            }
        }
        return photoFile;
    }
}
