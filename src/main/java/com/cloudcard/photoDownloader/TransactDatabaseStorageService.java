package com.cloudcard.photoDownloader;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class TransactDatabaseStorageService extends DatabaseStorageService {
    private static final Logger log = LoggerFactory.getLogger(TransactDatabaseStorageService.class);

    @Value("${downloader.minPhotoIdLength}")
    private Integer minPhotoIdLength;
    @Value("${db.mapping.table.photos}")
    String tableName;

    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {

        JdbcTemplate jdbcCustomerTable = new JdbcTemplate(dataSource);
        JdbcTemplate jdbcPhotoTable = new JdbcTemplate(customerDataSource);

        List<PhotoFile> photoFiles = new ArrayList<>();

        for (Photo photo : photos) {

            String personIdentifier = StringUtils.leftPad(photo.getPerson().getIdentifier(), minPhotoIdLength, '0');

//            TODO: Create SELECT query for getting needed customer info
            String customerQuery = "SELECT CUST_ID FROM " + tableName + " WHERE CUSTNUM = " + personIdentifier;

            TransactCustomer record = null;

            try {
                record = jdbcCustomerTable.queryForObject(customerQuery, new TransactCustomerMapper());
            } catch (EmptyResultDataAccessException e) {
                log.error("No record in DB for given id.");
            }

            if (record == null) {
                log.error("No record to update.");
            } else {
                String photoQuery = "UPDATE ";
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