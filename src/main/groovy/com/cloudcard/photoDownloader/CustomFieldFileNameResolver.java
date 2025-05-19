package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Map;

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue;

@Service
@ConditionalOnProperty(value = "downloader.fileNameResolver", havingValue = "CustomFieldFileNameResolver")
public class CustomFieldFileNameResolver implements FileNameResolver {

    private static final Logger log = LoggerFactory.getLogger(CustomFieldFileNameResolver.class);

    @Value("${downloader.minPhotoIdLength}")
    private Integer minPhotoIdLength;

    @Value("${CustomFieldFileNameResolver.include}")
    String[] include;

    @Value("${CustomFieldFileNameResolver.delimiter}")
    String delimiter;

    @PostConstruct
    void init() {
        throwIfTrue(include == null || include.length == 0, "The Custom Field(s) must be specified.");
        log.info("Include Custom Fields : " + include.length + " " + (Arrays.toString(include)));
        log.info("Custom Field Delimiter: '" + delimiter + "'");
    }

    @Override
    public String getBaseName(Photo photo) {
        Map<String, String> customFields = photo.getPerson().getCustomFields();
        String fileName = "";

        for (String i : include) {
            if (!fileName.equals("")) {
                fileName = fileName + delimiter;
            }
            if (i.equals("identifier")) {
                fileName = fileName + photo.getPerson().getIdentifier();
            } else {
                String customFieldValue = customFields.get(i);
                if (customFieldValue == null || customFieldValue.isEmpty()) {
                    log.error("Person: " + photo.getPerson().getEmail() + " is missing a value for " + i);
                    return null;
                }
                fileName = fileName + customFieldValue;
            }
        }

        return fileName;
    }

}
