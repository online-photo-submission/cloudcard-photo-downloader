package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;

//@Service
public class SimpleDatabaseStorageService extends DatabaseStorageService {
    private static final Logger log = LoggerFactory.getLogger(SimpleDatabaseStorageService.class);

    @Value("${db.mapping.column.studentId}")
    String studentIdColumnName;
    @Value("${db.mapping.column.photoId}")
    String photoColumnName;
    @Value("${db.mapping.table}")
    String tableName;

    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {
        List<PhotoFile> photoFiles = new ArrayList<>();

        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(dataSource).withTableName(tableName);

        for (Photo photo: photos) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(studentIdColumnName, photo.getPerson().getIdentifier());
            parameters.put(photoColumnName, photo.getBytes());

            try {
                simpleJdbcInsert.execute(parameters);
                photoFiles.add(new PhotoFile(photo.getPerson().getIdentifier(), null, photo.getId()));
            } catch (Exception e) {
                log.error("Failed to push photo" + photo.getId() + " to DB.");
                e.printStackTrace();
            }

        }

        return photoFiles;
    }


}
