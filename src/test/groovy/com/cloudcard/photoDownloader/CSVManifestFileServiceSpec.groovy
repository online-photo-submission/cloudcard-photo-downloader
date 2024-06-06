package com.cloudcard.photoDownloader

import org.springframework.boot.test.context.SpringBootTest
import spock.lang.Specification
import spock.lang.Unroll

import java.text.SimpleDateFormat

@SpringBootTest
class CSVManifestFileServiceSpec extends Specification {

    CSVManifestFileService csvManifestFileService

    def setup() {
        csvManifestFileService = new CSVManifestFileService()
        csvManifestFileService.directory = "./"
        csvManifestFileService.fileName = "test-manifest-"
        csvManifestFileService.fileNameDateFormat = "yyyyMMdd-HHmm"
        csvManifestFileService.delimiter = ','
        csvManifestFileService.quoteMode = "ALL_NON_NULL"
        csvManifestFileService.quoteCharacter = '"'
        csvManifestFileService.escapeCharacter = '\\'
        csvManifestFileService.dateFormat = "yyyy-MM-dd"
        csvManifestFileService.headerAndColumnMap = [
            "personID": "person.identifier",
            "source": "static_CloudCard",
            "PhotoLink": "bytes",
            "DateSubmitted": "dateSubmitted"
        ]
    }

    def "test createManifestFile generates file with correct content"() {
        given: "A list of photos and photoFiles to download"
        List<Photo> photosToDownload = [
            new Photo(id: 1, person: new Person(identifier: "123", email: "test1@example.com"), bytes: null),
            new Photo(id: 2, person: new Person(identifier: "456", email: "test2@example.com"), bytes: null)
        ]

        List<PhotoFile> photoFiles = [
            new PhotoFile("photo1", "photo1.jpg", 1),
            new PhotoFile("photo2", "photo2.jpg", 2)
        ]
        String expectedFilePath = csvManifestFileService.directory + "/" + csvManifestFileService.resolveFileName()
        File expectedFile = new File(expectedFilePath)

        when: "createManifestFile is called"
        csvManifestFileService.createManifestFile(photosToDownload, photoFiles)

        then: "A manifest file is generated with the correct content"
        expectedFile.exists()
        List<String> lines = expectedFile.readLines()
        lines.size() == 3
        lines[1] == '"123","CloudCard",,'
        lines[2] == '"456","CloudCard",,'

        cleanup:
        expectedFile.delete()
    }

    @Unroll
    def "test resolveValue returns #expectedResult for #description"() {
        given:
        csvManifestFileService.dateFormat = "yyyy-MM-dd"

        when:
        def result = csvManifestFileService.resolveValue(column, photo, photoFile)

        then:
        result == expectedResult
        where:
        description             | column                          | photo                                                                                                                                                   | photoFile                                                                | expectedResult
        "static column"         | "static_CloudCard"              | null                                                                                                                                                    | null                                                                     | "CloudCard"
        "nested property"       | "person.email"                  | new Photo(id: 1, person: new Person(identifier: "123", email: "test@example.com"), bytes: null)                                                         | null                                                                     | "test@example.com"
        "non-existent property" | "person.nonExistentProperty"    | new Photo(id: 1, person: new Person(identifier: "123", email: "test@example.com"), bytes: null)                                                         | null                                                                     | null
        "date property"         | "dateCreated"                   | new Photo(id: 1, person: new Person(identifier: "123", email: "test@example.com"), bytes: null, dateCreated: parseDate("Fri Feb 16 11:41:44 EST 2024")) | null                                                                     | "2024-02-16"
        "custom Field property" | "person.customFields.Full Name" | new Photo(id: 1, person: new Person(identifier: "123", email: "test@example.com", customFields: ["Full Name": "James"]), bytes: null)                   | null                                                                     | "James"
        "photo full file path"  | "photo_fullFilePath"            | new Photo(id: 1, person: new Person(identifier: "123", email: "test@example.com"), bytes: null)                                                         | new PhotoFile("photo1", "/Users/Calvin/downloaded-photos/photo1.jpg", 1) | "/Users/Calvin/downloaded-photos/photo1.jpg"
        "photo file name"       | "photo_fileName"                | new Photo(id: 1, person: new Person(identifier: "123", email: "test@example.com"), bytes: null)                                                         | new PhotoFile("photo2", "photo2.jpg", 1)                                 | "photo2.jpg"
    }

    def "test resolveFileName generates correct file name"() {
        given: "An expected patter"
        String expectedPattern = csvManifestFileService.fileName + new SimpleDateFormat(csvManifestFileService.fileNameDateFormat).format(new Date()) + ".csv"

        when: "resolveFileName is called"
        String fileName = csvManifestFileService.resolveFileName()

        then: "The generated file name matches the expected pattern"
        fileName.endsWith(".csv")
        fileName ==~ expectedPattern
    }

    def "test formatDate formats date according to dateFormat"() {
        given: "A date and a dateFormat"
        Date date = new Date()
        csvManifestFileService.dateFormat = "yyyy-MM-dd"
        String expectedFormattedDate = new SimpleDateFormat(csvManifestFileService.dateFormat).format(date)

        when: "formatDate is called"
        String formattedDate = csvManifestFileService.formatDate(date)

        then: "The date is formatted according to the specified dateFormat"
        formattedDate == expectedFormattedDate
    }

/* PRIVATE HELPER METHODS */
    private static Date parseDate(String dateString) {
        new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy").parse(dateString)
    }
}