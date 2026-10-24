package com.cloudcard.photoDownloader

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(value = "downloader.postProcessor", havingValue = "AdditionalPhotoPostProcessor")
class AdditionalPhotoPostProcessor implements PostProcessor {

    private static final Logger log = LoggerFactory.getLogger(AdditionalPhotoPostProcessor.class);

    @Autowired
    FileService fileService;

    @Autowired
    CloudCardClient cloudCardClient;

    @Value('${AdditionalPhotoPostProcessor.include}')
    String[] include;

    @Override
    PhotoFile process(Photo photo, String photoDirectory, PhotoFile photoFile) {

        for (AdditionalPhoto additionalPhoto : photo.person?.additionalPhotos) {

            if(include != null && !Arrays.asList(include).contains(additionalPhoto.typeName)) {
                continue; //skip this one
            }

            String directoryName = photoDirectory + "/" + additionalPhoto.typeName;
            try {
                cloudCardClient.fetchBytes(additionalPhoto);
                fileService.writeBytesToFile(directoryName, photoFile.baseName + ".jpg", additionalPhoto.bytes);
            } catch (Exception e) {
                log.error("Failed to save additional photo ($additionalPhoto.typeName) for person ${photo.person?.identifier}", e)
            }
        }
        return photoFile;
    }
}
