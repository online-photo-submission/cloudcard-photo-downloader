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

        JdbcTemplate jdbcCustomerTable = new JdbcTemplate(dataSource);
        NamedParameterJdbcTemplate jdbcPhotoTable = new NamedParameterJdbcTemplate(customerDataSource);

        List<PhotoFile> photoFiles = new ArrayList<>();

        for (Photo photo : photos) {

            String personIdentifier = StringUtils.leftPad(photo.getPerson().getIdentifier(), minPhotoIdLength, '0');

//            TODO: Create SELECT query for getting needed customer info
            String customerQuery = "SELECT CUST_ID FROM " + lookupTableName + " WHERE CUSTNUM = " + personIdentifier;

            int custId = -1;

            try {
                custId = jdbcCustomerTable.queryForObject(customerQuery, Integer.class);
            } catch (EmptyResultDataAccessException e) {
                log.error("No record in DB for given id.");
            }

            if (custId == -1) {
                log.error("No record to update.");
            } else {
//                String photoQuery = "UPDATE " + tableName + " SET PHOTO = " + photo.getBytes() + "] WHERE CUST_ID = " + custId;
                String preparedPhotoQuery = "UPDATE " + tableName + " SET PHOTO = :photoBytes WHERE CUST_ID = :custId" ;
                MapSqlParameterSource params = new MapSqlParameterSource();
                params.addValue("tableName", tableName);
                params.addValue("photoBytes", new SqlLobValue(new ByteArrayInputStream(photo.getBytes()), photo.getBytes().length, new DefaultLobHandler()), OracleTypes.BLOB);
                params.addValue("custId", custId);
                try {
                    jdbcPhotoTable.update(preparedPhotoQuery, params);
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