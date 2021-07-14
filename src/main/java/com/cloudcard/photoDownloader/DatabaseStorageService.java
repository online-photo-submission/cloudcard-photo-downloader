package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.support.SqlLobValue;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.ByteArrayInputStream;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "DatabaseStorageService")
public class DatabaseStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(DatabaseStorageService.class);

    @Value("${db.mapping.column.studentId}")
    String studentIdColumnName;
    @Value("${db.mapping.column.photoId}")
    String photoColumnName;
    @Value("${db.mapping.table}")
    String tableName;

    @Autowired
    DataSource dataSource;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private FileNameResolver fileNameResolver;

    JdbcTemplate jdbcCustomerTable;
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    String sql;

    @PostConstruct
    void init() {

        jdbcCustomerTable = new JdbcTemplate(dataSource);
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        sql = "insert into " + tableName + " (" + studentIdColumnName + ", " + photoColumnName + ") VALUES(:id, :photo)";

        log.info("   File Name Resolver : " + fileNameResolver.getClass().getSimpleName());
        log.info("           Insert SQL : " + sql);
    }


    @Override
    public List<PhotoFile> save(Collection<Photo> photos) {

        List<PhotoFile> photoFiles = new ArrayList<>();

        for (Photo photo : photos) {
            try {
                String fileName = fileNameResolver.getBaseName(photo);

                if (fileName == null || fileName.isEmpty()) {
                    log.error("We could not resolve the base file name for '" + photo.getPerson().getEmail() + "' with ID number '"
                        + photo.getPerson().getIdentifier() + "', so photo " + photo.getId() + " cannot be saved.");
                    continue;
                }

                if (photo.getBytes() == null) {
                    log.error("Photo " + photo.getId() + " for " + photo.getPerson().getEmail() + " is missing binary data, so it cannot be saved.");
                    continue;
                }

                MapSqlParameterSource insertParams = createInsertParams(photo, fileName);
                namedParameterJdbcTemplate.update(preparePhotoQuery(photo.getPerson().getIdentifier(), fileName), insertParams);

                photoFiles.add(new PhotoFile(photo.getPerson().getIdentifier(), null, photo.getId()));
            } catch (Exception e) {
                log.error("Failed to push photo" + photo.getId() + " to DB.");
                e.printStackTrace();
            }

        }

        return photoFiles;
    }

    /* *** PRIVATE HELPERS *** */

    private MapSqlParameterSource createInsertParams(Photo photo, String fileName) {

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", fileName);
        in.addValue("photo", new SqlLobValue(new ByteArrayInputStream(photo.getBytes()),
            photo.getBytes().length, new DefaultLobHandler()), Types.BLOB);
        return in;
    }

    private String preparePhotoQuery(String personIdentifier, String fileName) {
        String preparedPhotoQuery;
        String photoCountQuery = "SELECT COUNT(" + studentIdColumnName + ") FROM " + tableName + "  where " + studentIdColumnName + " = " + fileName;
        int rowCount = jdbcCustomerTable.queryForObject(photoCountQuery, Integer.class);
        if (rowCount == 0) {
            log.info("Preparing insert for : " + personIdentifier);
            preparedPhotoQuery = "insert into " + tableName + " (" + studentIdColumnName + ", " + photoColumnName + ") VALUES(:id, :photo)";
        } else {
            log.info("Preparing update for : " + personIdentifier);
            preparedPhotoQuery = "UPDATE " + tableName + " SET " + photoColumnName + " = :photo WHERE " + studentIdColumnName + " = :id";
        }
        return preparedPhotoQuery;
    }
}
