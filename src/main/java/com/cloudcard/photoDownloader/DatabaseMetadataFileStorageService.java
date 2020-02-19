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

    @Value("${downloader.sql.photoField.filePath:}")
    String photoFieldFilePath;


    @Autowired
    JdbcTemplate jdbcTemplate;

    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {

        List<PhotoFile> photoFiles = super.save(photos);

        return photoFiles;
    }

    @Override
    protected String getBaseName(Photo photo) {

        Person person = photo.getPerson();
        log.info("Downloading photo for person: " + person.getIdentifier());

        String query = "select top 1 firstname, lastname, IDNumber, ssnnumber, photoupdated, expirationdate " +
            "from WILDCARD where NetID = '" + person.getIdentifier() + "' and SSNNumber like '99%' " +
            "order by expirationdate desc";

        NorthwesternPersonRecord record = null;
        try {
            record = jdbcTemplate.queryForObject(query, new NorthwesternPersonRecordMapper());
        } catch (EmptyResultDataAccessException e) {
            log.error("No record in database for ID: " + person.getIdentifier());
        }

        if (record == null) {
            log.error("Null record returned from database for ID: " + person.getIdentifier());
            return null;
        }

        return record.getPrefixedIdNumber();
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
