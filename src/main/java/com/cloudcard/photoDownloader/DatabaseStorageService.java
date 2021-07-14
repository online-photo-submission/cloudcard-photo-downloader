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

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @PostConstruct
    void init() {
        log.info("   File Name Resolver : " + fileNameResolver.getClass().getSimpleName());
        log.info("           Insert SQL : " + insertSql());
        log.info("           Update SQL : " + updateSql());
    }


    @Override
    public List<PhotoFile> save(Collection<Photo> photos) {

        List<PhotoFile> photoFiles = new ArrayList<>();

        for (Photo photo : photos) {
            PhotoFile photoFile = save(photo);
            if (photoFile != null) photoFiles.add(photoFile);
        }

        return photoFiles;
    }

    private PhotoFile save(Photo photo) {

        try {
            String dbIdentifier = fileNameResolver.getBaseName(photo);

            if (dbIdentifier == null || dbIdentifier.isEmpty()) {
                log.error("We could not resolve the base file name for '" + photo.getPerson().getEmail() + "' with ID number '"
                    + photo.getPerson().getIdentifier() + "', so photo " + photo.getId() + " cannot be saved.");
                return null;
            }

            if (photo.getBytes() == null) {
                log.error("Photo " + photo.getId() + " for " + photo.getPerson().getEmail() + " is missing binary data, so it cannot be saved.");
                return null;
            }

            MapSqlParameterSource insertParams = createInsertParams(photo, dbIdentifier);
            namedParameterJdbcTemplate.update(preparePhotoQuery(photo.getPerson().getIdentifier(), dbIdentifier), insertParams);

            return new PhotoFile(photo.getPerson().getIdentifier(), null, photo.getId());
        } catch (Exception e) {
            log.error("Failed to push photo" + photo.getId() + " to DB.");
            e.printStackTrace();
            return null;
        }
    }

    /* *** PRIVATE HELPERS *** */

    private MapSqlParameterSource createInsertParams(Photo photo, String fileName) {

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", fileName);
        in.addValue("photo", new SqlLobValue(new ByteArrayInputStream(photo.getBytes()),
            photo.getBytes().length, new DefaultLobHandler()), Types.BLOB);
        return in;
    }

    private String preparePhotoQuery(String personIdentifier, String dbIdentifier) {
        String preparedPhotoQuery;
        String photoCountQuery = "SELECT COUNT(" + studentIdColumnName + ") FROM " + tableName + "  where " + studentIdColumnName + " = " + dbIdentifier;
        int rowCount = jdbcTemplate.queryForObject(photoCountQuery, Integer.class);
        if (rowCount == 0) {
            log.info("Preparing insert for : " + personIdentifier);
            preparedPhotoQuery = insertSql();
        } else {
            log.info("Preparing update for : " + personIdentifier);
            preparedPhotoQuery = updateSql();
        }
        return preparedPhotoQuery;
    }

    private String updateSql() {
        return "UPDATE " + tableName + " SET " + photoColumnName + " = :photo WHERE " + studentIdColumnName + " = :id";
    }

    private String insertSql() {
        return "INSERT INTO " + tableName + " (" + studentIdColumnName + ", " + photoColumnName + ") VALUES(:id, :photo)";
    }
}
