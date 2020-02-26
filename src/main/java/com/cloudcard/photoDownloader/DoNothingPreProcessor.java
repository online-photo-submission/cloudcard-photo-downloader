package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "downloader.preProcessor", havingValue = "DoNothingPreProcessor", matchIfMissing = true)
public class DoNothingPreProcessor implements PreProcessor {

    private static final Logger log = LoggerFactory.getLogger(DoNothingPreProcessor.class);

    @Override
    public Photo process(Photo photo) {

        // do nothing
        return photo;
    }

}
