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

    @Autowired
    DataSource dataSource;

    NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    String sql;

    @PostConstruct
    void init() {

        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        sql = "insert into " + tableName + " (" + studentIdColumnName + ", " + photoColumnName + ") VALUES(:id, :photo)";
    }


    @Override
    public List<PhotoFile> save(Collection<Photo> photos) {

        List<PhotoFile> photoFiles = new ArrayList<>();

        for (Photo photo : photos) {
            try {
                MapSqlParameterSource insertParams = createInsertParams(photo);
                namedParameterJdbcTemplate.update(sql, insertParams);

                photoFiles.add(new PhotoFile(photo.getPerson().getIdentifier(), null, photo.getId()));
            } catch (Exception e) {
                log.error("Failed to push photo" + photo.getId() + " to DB.");
                e.printStackTrace();
            }

        }

        return photoFiles;
    }

    /* *** PRIVATE HELPERS *** */

    private MapSqlParameterSource createInsertParams(Photo photo) {

        MapSqlParameterSource in = new MapSqlParameterSource();
        in.addValue("id", photo.getPerson().getIdentifier());
        in.addValue("photo", new SqlLobValue(new ByteArrayInputStream(photo.getBytes()),
            photo.getBytes().length, new DefaultLobHandler()), Types.BLOB);
        return in;
    }
}
