package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Service
public class NorthwesternStorageService extends DatabaseStorageService {
    private static final Logger log = LoggerFactory.getLogger(NorthwesternStorageService.class);

    @Value("${cloudcard.api.url}")
    private String apiUrl;
    @Value("${cloudcard.api.accessToken}")
    private String accessToken;

    @Value("${db.mapping.column.studentId}")
    String studentIdColumnName;
    @Value("${db.mapping.column.photoId}")
    String photoColumnName;
    @Value("${db.mapping.table}")
    String tableName;

//    @Value("${downloader.photoDirectories}")
//    String[] photoDirectories;

    String photoDirectoryUpdate = ".";
    String photoDirectoryNoUpdate = ".";
    String photoDirectoryError = ".";

    @Value("${downloader.minPhotoIdLength}")
    Integer minPhotoIdLength;

    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<PhotoFile> photoFiles = new ArrayList<>();

//        Logic for retrieving records for students who need photos (JDBC)
//        String oldQuery = "select idnumber,ssnnumber,firstname,lastname,picture,photoupdated from WILDCARD where SSNNumber = '99' + idnumber";

        for (Photo photo: photos) {
            Person person = photo.getPerson();
            String query = "select photoupdated from WILDCARD where SSNNumber = '99' + " + person.getIdentifier();
//            String query = "select firstname, lastname, idnumber, photoupdated from WILDCARD where idnumber = '1012922'";
            log.info(query);

            NorthwesternPersonRecord record = jdbcTemplate.queryForObject(query, new NorthwesternPersonRecordMapper());

            if (record.needsUpdate()) {
                photoFiles.add(saveToFile(photo, photoDirectoryUpdate, record.getIdentifier(), "99" + record.getIdentifier()));
            }
            else if (!record.needsUpdate()) {
                photoFiles.add(saveToFile(photo, photoDirectoryNoUpdate, record.getIdentifier(), record.getIdentifier()));
            }
            else {
                photoFiles.add(saveToFile(photo, photoDirectoryError,null, person.getEmail()));
            }
        }

        return photoFiles;
    }

    PhotoFile saveToFile(Photo photo, String photoDirectory, String studentID, String filename) throws Exception {

        if (studentID == null || studentID.isEmpty()) {
            log.error(photo.getPerson().getEmail() + " is missing an ID number, so photo " + photo.getId() + " cannot be saved.");
            return null;
        }

        if (photo.getBytes() == null) {
            log.error("Photo " + photo.getId() + " for " + photo.getPerson().getEmail() + " is missing binary data, so it cannot be saved.");
            return null;
        }

        String fileName = writeBytesToFile(photoDirectory, filename + ".jpg", photo.getBytes());

        return new PhotoFile(studentID, fileName, photo.getId());
    }

    String writeBytesToFile(String directoryName, String fileName, byte[] bytes) throws IOException {

        File directory = new File(directoryName);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        File file = new File(directoryName + File.separator + fileName);

        FileOutputStream outputStream = new FileOutputStream(file);

        if (!file.exists()) {
            file.createNewFile();
        }
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();

        return file.getCanonicalPath();
    }

    void getPersonCustomFields(Person person) throws Exception {
        JsonFactory jsonFactory = new JsonFactory();

        HttpResponse<String> response = Unirest.get(apiUrl + "/people/" + person.getIdentifier()).headers(standardHeaders()).asString();

        JsonParser parser = jsonFactory.createParser(response.getBody());
        while(!parser.isClosed()) {
            JsonToken jsonToken = parser.nextToken();

            if (jsonToken.FIELD_NAME.equals(jsonToken)) {
                String fieldName = parser.getCurrentName();

            }
        }
    }

    private Map<String, String> standardHeaders() {

        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("X-Auth-Token", accessToken);
        return headers;
    }


}
