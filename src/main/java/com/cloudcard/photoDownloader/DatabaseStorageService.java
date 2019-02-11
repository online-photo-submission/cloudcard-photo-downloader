package com.cloudcard.photoDownloader;

import org.springframework.jdbc.core.simple.SimpleJdbcInsert;

import javax.sql.DataSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseStorageService implements StorageService{

    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {
        return null;
    }

    public void save(Collection<Photo> photos, DataSource dataSource) throws Exception {
        SimpleJdbcInsert simpleJdbcInsert = new SimpleJdbcInsert(dataSource).withTableName("photos").usingGeneratedKeyColumns("ID");

        for (Photo photo: photos) {
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("STUDENT_ID", photo.getPerson().getIdentifier());
            parameters.put("PHOTO", photo.getBytes());

            simpleJdbcInsert.execute(parameters);
        }
    }

}
