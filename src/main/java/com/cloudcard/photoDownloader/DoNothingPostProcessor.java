package com.cloudcard.photoDownloader;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "downloader.postProcessor", havingValue = "DoNothingPostProcessor")
public class DoNothingPostProcessor implements PostProcessor {

    @Override
    public PhotoFile process(Photo photo, String photoDirectory, PhotoFile photoFile) {
        // do nothing
        return photoFile;
    }

}
