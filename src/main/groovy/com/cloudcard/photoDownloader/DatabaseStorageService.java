package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
    @Value("${db.photoUpdates.enabled:true}")
    Boolean updateExistingPhoto;

    @Autowired
    DataSource dataSource;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private FileNameResolver fileNameResolver;

    @Autowired
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    private PostProcessor postProcessor;

    @PostConstruct
    void init() {
        log.info("   File Name Resolver : " + fileNameResolver.getClass().getSimpleName());
        log.info("           Count  SQL : " + countSql());
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

            persistPhoto(photo, dbIdentifier);

//          passing in null photoDirectory and fileName because they don't exist when storing photos in a DB
            postProcessor.process(photo, null, new PhotoFile(dbIdentifier, null, photo.getId()));

            return new PhotoFile(photo.getPerson().getIdentifier(), null, photo.getId());
        } catch (Exception e) {
            log.error("Failed to push photo" + photo.getId() + " to DB.");
            e.printStackTrace();
            return null;
        }
    }

    /* *** PRIVATE HELPERS *** */

    private int persistPhoto(Photo photo, String dbIdentifier) {
        boolean isInsert = isInsert(dbIdentifier);
    
        log.info("Performing " + (isInsert ? "insert" : "update") + " for : " + photo.getPerson().getIdentifier());

        String query = isInsert ? insertSql() : updateSql();
                
        return namedParameterJdbcTemplate.update(query, buildParams(photo, dbIdentifier));
    }


    private MapSqlParameterSource buildParams(Photo photo, String fileName) {
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("id", fileName);
        params.addValue("photo", new SqlLobValue(new ByteArrayInputStream(photo.getBytes()),
            photo.getBytes().length, new DefaultLobHandler()), Types.BLOB);
        return params;
    }

    /**
     * Determine if we need to insert a new record or update an existing record by checking if a record already exists for the given identifier 
     */
    private boolean isInsert(String dbIdentifier) {
        if (!updateExistingPhoto) return true;

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", dbIdentifier);

        int rowCount = namedParameterJdbcTemplate.queryForObject(countSql(), in, Integer.class);

        return rowCount == 0;
    }

    private String countSql() {
        return "SELECT COUNT(" + studentIdColumnName + ") FROM " + tableName + "  where " + studentIdColumnName + " = :id";
    }

    private String updateSql() {
        return "UPDATE " + tableName + " SET " + photoColumnName + " = :photo WHERE " + studentIdColumnName + " = :id";
    }

    private String insertSql() {
        return "INSERT INTO " + tableName + " (" + studentIdColumnName + ", " + photoColumnName + ") VALUES(:id, :photo)";
    }
}
