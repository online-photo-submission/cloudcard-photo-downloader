package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
    @Value("${downloader.photoDirectoryError:}")
    String photoDirectoryError;

    @Value("${downloader.sql.photoField.filePath:}")
    String photoFieldFilePath;

    @Value("${downloader.minPhotoIdLength}")
    Integer minPhotoIdLength;

    @Override
    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        List<PhotoFile> photoFiles = new ArrayList<>();

        for (Photo photo : photos) {
            Person person = photo.getPerson();
            log.info("Downloading photo for person: " + person.getIdentifier());

            String query = "select top 1 firstname, lastname, IDNumber, ssnnumber, photoupdated, expirationdate " +
                "from WILDCARD where NetID = '" + person.getIdentifier() + "' and SSNNumber like '99%' " +
                "order by expirationdate desc";

            NorthwesternPersonRecord record = null;
            try {
                record = jdbcTemplate.queryForObject(query, new NorthwesternPersonRecordMapper());
            } catch (EmptyResultDataAccessException e) {
                log.error("No record in database for ID: " + person.getIdentifier());
            }

            if (record == null) {
                log.error("Null record returned from database for ID: " + person.getIdentifier());
                photoFiles.add(saveToFile(photo, photoDirectoryError, person.getIdentifier(), person.getIdentifier()));
            } else {
                log.info("Person " + person.getIdentifier() + " has an expiration date of " + record.getExpirationDate());
                PhotoFile file;
                if (!record.needsCardPhoto()) {
                    log.info("Saving person " + person.getIdentifier() + " as Outlook");
                    file = saveToFile(photo, photoDirectoryOutlook, record.getPrefixedIdNumber(), record.getIdNumber());
                    photoFiles.add(file);
                    //                    updateDatabase(jdbcTemplate, person, file, photoDirectoryOutlook);
                } else {
                    log.info("Saving person " + person.getIdentifier() + " as Wildcard");
                    file = saveToFile(photo, photoDirectoryWildcard, record.getPrefixedIdNumber(), record.getPrefixedIdNumber());
                    photoFiles.add(file);
                    updateDatabase(jdbcTemplate, person, file, photoDirectoryWildcard);
                }
                log.info("Saved photo for person " + person.getIdentifier() + " to path " + file.getFileName());
            }
        }

        return photoFiles;
    }

    private void updateDatabase(JdbcTemplate jdbcTemplate, Person person, PhotoFile file, String photoDirectory) {

        if (file != null) {
            Timestamp photoUpdated = Timestamp.valueOf(LocalDateTime.now().withSecond(0).withNano(0));
            String fileName = photoFieldFilePath.equals("") ? file.getFileName() : photoFieldFilePath + file.getStudentId() + ".jpg";
            log.info("updating database: Picture = " + fileName + ", PhotoUpdated = " + photoUpdated.toString());
            try {
                jdbcTemplate.update("update WILDCARD set PhotoUpdated = '" + photoUpdated + "', Picture = '" + fileName + "' where NetID = '" + person.getIdentifier() + "'");
            } catch (Exception e) {
                log.error("Unable to push update to database for ID: '" + person.getIdentifier() + "'. " + e.getMessage());
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

        String savedFilePath = writeBytesToFile(photoDirectory, filename + ".jpg", photo.getBytes());


        return new PhotoFile(studentID, savedFilePath, photo.getId());
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

}
