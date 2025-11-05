package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

import java.text.SimpleDateFormat

import static com.cloudcard.photoDownloader.ApplicationPropertiesValidator.throwIfTrue

@Service
@ConditionalOnProperty(value = "downloader.fileNameResolver", havingValue = "DynamicFileNameResolver")
class DynamicFileNameResolver implements FileNameResolver {

    private static final Logger log = LoggerFactory.getLogger(DynamicFileNameResolver.class)

    @Value('${DynamicFileNameResolver.include}')
    String[] include

    @Value('${DynamicFileNameResolver.delimiter}')
    String delimiter

    @Value('${DynamicFileNameResolver.dateFormat:YYYY-MM-DD_HH-MM-SS}')
    String dateFormat

    @PostConstruct
    void init() {
        throwIfTrue(include == null || include.length == 0, "The Field(s) for the DynamicFileNameResolver must be specified.")
        log.info("Including Fields : " + include.length + " " + (Arrays.toString(include)))
        log.info("DynamicFileNameResolver  Delimiter: '" + delimiter + "'")
    }

    @Override
    String getBaseName(Photo photo) {
        Map<String, String> customFields = photo.getPerson().getCustomFields()
        String fileName = ""

        for (String i : include) {
            if (!fileName.equals("")) {
                fileName = fileName + delimiter
            }
            if (i.equals("dateCreated")) {
                fileName = fileName + new SimpleDateFormat(dateFormat).format(photo.dateCreated)
            } else if (i.equals("identifier")) {
                fileName = fileName + photo.getPerson().getIdentifier()
            } else {
                String customFieldValue = customFields.get(i)
                if (customFieldValue == null || customFieldValue.isEmpty()) {
                    log.error("Person: " + photo.getPerson().getEmail() + " is missing a value for " + i)
                    return null
                }
                fileName = fileName + customFieldValue
            }
        }
        return fileName
    }

}
