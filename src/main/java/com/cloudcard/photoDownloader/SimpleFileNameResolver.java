package com.cloudcard.photoDownloader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "downloader.fileNameResolver", havingValue = "SimpleFileNameResolver")
public class SimpleFileNameResolver implements FileNameResolver {

    @Value("${downloader.minPhotoIdLength}")
    private Integer minPhotoIdLength;

    @Override
    public String getBaseName(Photo photo) {

        String identifier = photo.getPerson().getIdentifier();
        if (identifier == null || identifier.isEmpty() || minPhotoIdLength < identifier.length()) return identifier;
        return String.format("%" + minPhotoIdLength + "s", identifier).replace(' ', '0');
    }

}
