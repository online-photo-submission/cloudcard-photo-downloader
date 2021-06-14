package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@ConditionalOnProperty(value = "downloader.postProcessor", havingValue = "AdditionalPhotoPostProcessor")
public class AdditionalPhotoPostProcessor implements PostProcessor {

    private static final Logger log = LoggerFactory.getLogger(AdditionalPhotoPostProcessor.class);

    @Autowired
    FileService fileService;

    @Autowired
    RestService restService;

    @Override
    public PhotoFile process(Photo photo, String photoDirectory, PhotoFile photoFile) {

        log.error("doing the post-processing");
        for (AdditionalPhoto additionalPhoto : photo.getPerson().getAdditionalPhotos()) {
            String directoryName = photoDirectory + "/" + additionalPhoto.getTypeName();
            try {
                restService.fetchBytes(additionalPhoto);
                // TODO: save all of the additional photos that exist and that are defined in the config value xxx.yyy.zzz
                fileService.writeBytesToFile(directoryName, photoFile.getBaseName() + ".jpg", additionalPhoto.getBytes());
            } catch (Exception e) {
                log.error(e.getMessage());
                log.error("Failed to save additional photo (" + additionalPhoto.getTypeName() + ") for person " + photo.getPerson().getIdentifier());
            }
        }
        return photoFile;
    }
}
