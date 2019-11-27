package com.cloudcard.photoDownloader;

import oracle.jdbc.internal.OracleTypes;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class TransactDatabaseStorageService extends DatabaseStorageService {

    private static final Logger log = LoggerFactory.getLogger(TransactDatabaseStorageService.class);

    @Value("${downloader.minPhotoIdLength}")
    private Integer minPhotoIdLength;
    @Value("${db.mapping.table.photos}")
    String tableName;
    @Value("${db.mapping.table.lookup}")
    String lookupTableName;


    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {

        //TODO: can these use the same datasource/template
        //TODO: can they be class properties
        //TODO: can they be injected
        JdbcTemplate jdbcCustomerTable = new JdbcTemplate(dataSource);
        NamedParameterJdbcTemplate jdbcPhotoTable = new NamedParameterJdbcTemplate(customerDataSource);

        List<PhotoFile> photoFiles = new ArrayList<>();

        for (Photo photo : photos) {

            save(photo, jdbcCustomerTable, jdbcPhotoTable, photoFiles);
        }
        return photoFiles;
    }

    /* *** PRIVATE HELPERS *** */
    private void save(Photo photo, JdbcTemplate jdbcCustomerTable, NamedParameterJdbcTemplate jdbcPhotoTable, List<PhotoFile> photoFiles) {

        String personIdentifier = padPersonIdentifier(photo);
        Integer custId = fetchCustId(jdbcCustomerTable, personIdentifier);

        if (custId == null) {
            log.error("No record in " + lookupTableName + " for '" + personIdentifier + "'.");
            return;
        }

        String preparedPhotoQuery = preparePhotoQuery(jdbcCustomerTable, personIdentifier, custId);
        MapSqlParameterSource params = prepareParams(photo, custId);
        try {
            jdbcPhotoTable.update(preparedPhotoQuery, params);
            //                    TODO: This should work but is untested. Verify before shipping
            photoFiles.add(new PhotoFile(photo.getPerson().getIdentifier(), null, photo.getId()));
            log.info("Downloaded: " + personIdentifier);
        } catch (Exception e) {
            log.error("Failed to download photo for '" + personIdentifier + "' to table '" + tableName + "'. Reason: " + e.getMessage() + "\nStacktrace Follows:\n" + Arrays.toString(e.getStackTrace()));
        }

    }

    private String padPersonIdentifier(Photo photo) {

        return StringUtils.leftPad(photo.getPerson().getIdentifier(), minPhotoIdLength, '0');
    }

    private MapSqlParameterSource prepareParams(Photo photo, Integer custId) {

        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("tableName", tableName);
        params.addValue("photoBytes", new SqlLobValue(new ByteArrayInputStream(photo.getBytes()), photo.getBytes().length, new DefaultLobHandler()), OracleTypes.BLOB);
        params.addValue("custId", custId);
        return params;
    }

    private Integer fetchCustId(JdbcTemplate jdbcCustomerTable, String personIdentifier) {

        try {
            String customerQuery = "SELECT CUST_ID FROM " + lookupTableName + " WHERE CUSTNUM = " + personIdentifier;
            return jdbcCustomerTable.queryForObject(customerQuery, Integer.class);
        } catch (EmptyResultDataAccessException e) {
            log.error("EmptyResultDataAccessException: " + e.getMessage());
            return null;
        }
    }

    private String preparePhotoQuery(JdbcTemplate jdbcCustomerTable, String personIdentifier, int custId) {

        String preparedPhotoQuery;
        String photoCountQuery = "SELECT COUNT(CUST_ID) FROM " + tableName + "  where CUST_ID = " + custId;
        int rowCount = jdbcCustomerTable.queryForObject(photoCountQuery, Integer.class);
        if (rowCount == 0) {
            log.info("Preparing insert for : " + personIdentifier);
            preparedPhotoQuery = "INSERT INTO " + tableName + " (CUST_ID, PHOTO) VALUES (:custId, :photoBytes)";
        } else {
            log.info("Preparing update for : " + personIdentifier);
            preparedPhotoQuery = "UPDATE " + tableName + " SET PHOTO = :photoBytes WHERE CUST_ID = :custId";
        }
        return preparedPhotoQuery;
    }
}