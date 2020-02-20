package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "downloader.fileNameResolver", havingValue = "DatabaseFileNameResolver")
public class DatabaseFileNameResolver implements FileNameResolver {

    private static final Logger log = LoggerFactory.getLogger(DatabaseFileNameResolver.class);

    @Value("${downloader.sql.query.baseFileName:}")
    String baseFileNameQuery;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public String getBaseName(Photo photo) {

        String identifier = photo.getPerson().getIdentifier();
        if (baseFileNameQuery.isEmpty()) {
            return identifier;
        }

        String baseName = null;
        try {
            baseName = jdbcTemplate.queryForObject(baseFileNameQuery, new Object[]{identifier}, String.class);
        } catch (EmptyResultDataAccessException e) {
            log.error("No record in database for person: " + identifier);
        }

        if (baseName == null) {
            log.error("The base file name returned from database for person: '" + identifier + "' was NULL.");
            return null;
        }

        log.info("The base file name for person: '" + identifier + "' is: " + baseName);
        return baseName;
    }
}
