package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@ConditionalOnProperty(value = "downloader.preProcessor", havingValue = "BytesLinkPreProcessor")
public class BytesLinkPreProcessor implements PreProcessor {

    private static final Logger log = LoggerFactory.getLogger(BytesLinkPreProcessor.class);
    public static final String PUBLIC_KEY_TOKEN = "{publicKey}";

    @Value("${BytesLinkPreprocessor.urlTemplate:}")
    String urlTemplate;

    @Value("${BytesLinkPreprocessor.additionalPhotoUrlTemplate:}")
    String additionalPhotoUrlTemplate;

    @PostConstruct
    void init() {

        ApplicationPropertiesValidator.throwIfBlank(urlTemplate, "BytesLinkPreprocessor.urlTemplate cannot be blank in application.properties.");
    }

    public Photo process(Photo photo) {
        String bytesLink = urlTemplate.replace(PUBLIC_KEY_TOKEN, photo.getPublicKey());
        log.info("Rewriting the bytes link for photo '" + photo.getId() + "' to: " + bytesLink);
        photo.setExternalURL(bytesLink);

        if (additionalPhotoUrlTemplate == null) {
            return photo;
        }

        for (AdditionalPhoto additionalPhoto : photo.getPerson().getAdditionalPhotos()) {
            bytesLink = additionalPhotoUrlTemplate.replace(PUBLIC_KEY_TOKEN, additionalPhoto.getPublicKey());
            log.info("Rewriting the bytes link for additionalPhoto '" + additionalPhoto.getId() + "' to: " + bytesLink);
            additionalPhoto.setExternalURL(bytesLink);
        }

        return photo;
    }
}
