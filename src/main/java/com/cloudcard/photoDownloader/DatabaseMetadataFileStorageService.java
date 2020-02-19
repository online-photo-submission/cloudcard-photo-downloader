package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Service
public class DatabaseMetadataFileStorageService extends FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseMetadataFileStorageService.class);

    @Value("${db.mapping.column.studentId}")
    String studentIdColumnName;
    @Value("${db.mapping.table}")
    String tableName;

    @Value("${downloader.metadata.overide.photoFilePath:}")
    String photoFieldFilePath;

    @Value("${downloader.sql.query.baseFileName:}")
    String baseFileNameQuery;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {

        List<PhotoFile> photoFiles = super.save(photos);

        return photoFiles;
    }

    @Override
    protected String getBaseName(Photo photo) {

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

    @Override
    protected void postProcess(Photo photo, String photoDirectory, PhotoFile photoFile) {

        updateDatabase(photo.getPerson().getIdentifier(), photoFile);
    }

    private void updateDatabase(String identifier, PhotoFile file) {

        if (file != null) {
            Timestamp photoUpdated = Timestamp.valueOf(LocalDateTime.now().withSecond(0).withNano(0));
            String fileName = photoFieldFilePath.equals("") ? file.getFileName() : photoFieldFilePath + file.getBaseName() + ".jpg";
            log.info("updating database: Picture = " + fileName + ", PhotoUpdated = " + photoUpdated.toString());
            try {
                jdbcTemplate.update("update WILDCARD set PhotoUpdated = '" + photoUpdated + "', Picture = '" + fileName + "' where NetID = '" + identifier + "'");
            } catch (Exception e) {
                log.error("Unable to push update to database for ID: '" + identifier + "'. " + e.getMessage());
            }
        }
    }

}
