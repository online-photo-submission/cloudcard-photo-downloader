package com.cloudcard.photoDownloader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.PermissionDeniedDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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

    @Value("${downloader.photoDirectoryWildcard}")
    String photoDirectoryWildcard;
    @Value("${downloader.photoDirectoryOutlook}")
    String photoDirectoryOutlook;
    @Value("${downloader.photoDirectoryError}")
    String photoDirectoryError;

    @Value("${downloader.minPhotoIdLength}")
    Integer minPhotoIdLength;

    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<PhotoFile> photoFiles = new ArrayList<>();

        for (Photo photo: photos) {
            Person person = photo.getPerson();

            person.setEmployeeNumber(getPersonEmployeeNumber(person, getPersonReport()));

            String query = "select firstname, lastname, ssnnumber, photoupdated, expirationdate from WILDCARD where ssnnumber = '" + person.getIdentifier() + "'";
            log.info(query);

            NorthwesternPersonRecord record = null;
            try {
                record = jdbcTemplate.queryForObject(query, new NorthwesternPersonRecordMapper());
            } catch(EmptyResultDataAccessException e) {
                log.error("No record in database for given id of " + person.getIdentifier());
            }

            if (record == null) {
                photoFiles.add(saveToFile(photo, photoDirectoryError, person.getIdentifier(), person.getIdentifier()));
            }
            else {
                if (!record.needsCardPhoto()) {
                    PhotoFile file = saveToFile(photo, photoDirectoryOutlook, record.getIdentifier(), person.getIdentifier());
                    photoFiles.add(file);
                    updateDatabase(jdbcTemplate, person, file, photoDirectoryOutlook);
                } else {
                    PhotoFile file = saveToFile(photo, photoDirectoryWildcard, record.getIdentifier(), "99" + person.getIdentifier());
                    photoFiles.add(file);
                    updateDatabase(jdbcTemplate, person, file, photoDirectoryWildcard);
                }
            }
        }

        return photoFiles;
    }

    private void updateDatabase(JdbcTemplate jdbcTemplate, Person person, PhotoFile file, String photoDirectory) {
        if (file != null) {
            Timestamp photoUpdated = Timestamp.valueOf(LocalDateTime.now().withSecond(0).withNano(0));
            log.info("updating database: Picture = " + photoDirectory + file.getFileName() + ", PhotoUpdated = " + photoUpdated.toString());

//            TODO: This fill-in query should work, but I'm having a tough time testing it, so the alternate query should work just as well.
//            jdbcTemplate.update("update WILDCARD set PhotoUpdated = ?, Picture = ? where IDNumber = ?", photoUpdated, photoDirectory + file.getFileName(), person.getIdentifier());
            try {
                jdbcTemplate.update("update WILDCARD set PhotoUpdated = '" + photoUpdated + "', Picture = '" + photoDirectory + file.getFileName() + "' where SSNNumber = '" + person.getIdentifier() + "'");
            } catch(Exception e) {
                log.error("Unable to push update to database: " + e.getMessage());
            }
        }
    }

    private PhotoFile saveToFile(Photo photo, String photoDirectory, String studentID, String filename) throws Exception {

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

    private String getPersonEmployeeNumber(Person person, PersonReportObject[] personRecord) throws Exception {
        String employeeNumber = null;
        for (PersonReportObject recordObject : personRecord) {
            if (recordObject.identifier.equals(person.getIdentifier())) employeeNumber = recordObject.employeeNumber;
        }
        if (employeeNumber != null) return employeeNumber;
        else throw new Exception("No employeeNumber found for person with identifier " + person.getIdentifier());
    }

    private PersonReportObject[] getPersonReport() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        HttpResponse<String> response = Unirest.get(apiUrl + "/reports/people").headers(standardHeaders()).asString();

        return mapper.readValue(response.getBody(), PersonReportObject[].class);
    }

//    TODO: Duplicate code from CloudCardPhotoService.java
    private Map<String, String> standardHeaders() {

        Map<String, String> headers = new HashMap<>();
        headers.put("accept", "application/json");
        headers.put("Content-Type", "application/json");
        headers.put("X-Auth-Token", accessToken);
        return headers;
    }


}
