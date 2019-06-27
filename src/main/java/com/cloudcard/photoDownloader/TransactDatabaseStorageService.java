package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TransactDatabaseStorageService extends DatabaseStorageService {
    private static final Logger log = LoggerFactory.getLogger(TransactDatabaseStorageService.class);


    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {

        JdbcTemplate jdbcCustomerTable = new JdbcTemplate(dataSource);
        JdbcTemplate jdbcPhotoTable = new JdbcTemplate(customerDataSource);

        List<PhotoFile> photoFiles = new ArrayList<>();

        for (Photo photo : photos) {

//            TODO: Create SELECT query for getting needed customer info
            String customerQuery = "";

            TransactCustomer record = null;

            try {
                record = jdbcCustomerTable.queryForObject(customerQuery, new TransactCustomerMapper());
            } catch (EmptyResultDataAccessException e) {
                log.error("No record in DB for given id.");
            }

            if (record == null) {
                log.error("No record to update.");
            } else {
//                TODO: Create MERGE statement (https://t5.si/f2418) for inserting/updating records
                String photoQuery = "";
                try {
                    jdbcPhotoTable.update(photoQuery);
//                    TODO: This should work but is untested. Verify before shipping
                    photoFiles.add(new PhotoFile(photo.getPerson().getIdentifier(), null, photo.getId()));
                } catch (Exception e) {
                    log.error("Failed to update photo table. Reason: " + e.getMessage() + ", Stacktrace: " + Arrays.toString(e.getStackTrace()));
                }
            }
        }
        return photoFiles;
    }
}