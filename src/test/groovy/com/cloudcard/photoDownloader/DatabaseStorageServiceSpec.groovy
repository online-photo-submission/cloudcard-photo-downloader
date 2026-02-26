package com.cloudcard.photoDownloader

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import spock.lang.Specification

class DatabaseStorageServiceSpec extends Specification {

    DatabaseStorageService service
    FileNameResolver fileNameResolver

    static JdbcTemplate jdbcTemplate
    static NamedParameterJdbcTemplate namedParameterJdbcTemplate

    String base64RedDot =   "/9j/4AAQSkZJRgABAQEAAAAAAAD/2wBDAAoHBwkHBgoJCAkLCwoMDxkQDw4ODx4WFxIZJCAmJSMgIyIoLTkwKCo2KyIjMkQyNjs9QEBAJjBGS0U+Sjk/QD3/2wBDAQsLCw8NDx0QEB09KSMpPT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT3/wAARCAAIAAgDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD9/KKKKAP/2Q=="
    String base64BlackDot = "/9j/4AAQSkZJRgABAQEAAAAAAAD/2wBDAAoHBwkHBgoJCAkLCwoMDxkQDw4ODx4WFxIZJCAmJSMgIyIoLTkwKCo2KyIjMkQyNjs9QEBAJjBGS0U+Sjk/QD3/2wBDAQsLCw8NDx0QEB09KSMpPT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT09PT3/wAARCAAIAAgDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFBAQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQAAAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRobHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwD9/KKKKAP/2Q=="
        
    def setupSpec() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource()
        dataSource.setDriverClassName("org.h2.Driver")
        dataSource.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
        dataSource.setUsername("sa")
        dataSource.setPassword("")

        jdbcTemplate = new JdbcTemplate(dataSource)

        jdbcTemplate.execute("""
            CREATE TABLE photos (
                student_id VARCHAR(255),
                photo_data BLOB
            )
        """)
    }

    def setup() {
        namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(jdbcTemplate)
        fileNameResolver = Mock(FileNameResolver)

        service = new DatabaseStorageService()
        service.postProcessor = new DoNothingPostProcessor()
        service.namedParameterJdbcTemplate = namedParameterJdbcTemplate
        service.fileNameResolver = fileNameResolver

        service.studentIdColumnName = "student_id"
        service.photoColumnName = "photo_data"
        service.tableName = "photos"
        service.updateExistingPhoto = true
    }

    void "test save with no photos"() {
        given:
        List<Photo> photos = [];

        when:
        StorageResults storageResults = service.save(photos);

        then: "no photos are saved"
        storageResults.downloadedPhotoFiles.size() == 0;

        and: "the filename resolver was not called"
        0 * service.fileNameResolver._
        
        and: "the database is empty"
        jdbcTemplate.queryForList("SELECT * FROM photos").size() == 0
    }

    void "test save with one good photo needing insert, then update it."() {
        given: "one photo with valid properties"
        String identifier = "B100012345"
        Photo photo = new Photo(
                id: 1,
                person: new Person(identifier: identifier, email: "$identifier@bacon.edu"),
                bytes: Base64.decoder.decode(base64RedDot)
        )
        List<Photo> photos = [ photo ]

        when: "we save the photo"
        StorageResults storageResults = service.save(photos)

        then: "photoFiles contains one PhotoFile with the correct properties"
        storageResults.downloadedPhotoFiles.size() == 1
        assert storageResults.downloadedPhotoFiles[0]
        storageResults.downloadedPhotoFiles[0].baseName == identifier
        storageResults.downloadedPhotoFiles[0].photoId == 1

        and: "the filename resolver was called"
        1 * service.fileNameResolver.getBaseName(photo) >> identifier

        and: "the photo was correctly inserted into the database"
        jdbcTemplate.queryForList("SELECT * FROM photos").size() == 1
        jdbcTemplate.queryForList("SELECT * FROM photos WHERE student_id = ?", identifier).size() == 1
        jdbcTemplate.queryForList("SELECT * FROM photos WHERE student_id = ?", identifier).get(0).get("photo_data") == photo.bytes

        when: "we update the photo bytes and save again"
        photo.bytes = Base64.decoder.decode(base64BlackDot)
        storageResults = service.save(photos)

        then: "photoFiles contains one PhotoFile with the correct properties"
        storageResults.downloadedPhotoFiles.size() == 1
        assert storageResults.downloadedPhotoFiles[0]
        storageResults.downloadedPhotoFiles[0].baseName == identifier
        storageResults.downloadedPhotoFiles[0].photoId == 1

        and: "the filename resolver was called"
        1 * service.fileNameResolver.getBaseName(photo) >> identifier

        and: "the photo was correctly updated in the database"
        jdbcTemplate.queryForList("SELECT * FROM photos").size() == 1
        jdbcTemplate.queryForList("SELECT * FROM photos WHERE student_id = ?", identifier).size() == 1
        jdbcTemplate.queryForList("SELECT * FROM photos WHERE student_id = ?", identifier).get(0).get("photo_data") == photo.bytes

    }

}