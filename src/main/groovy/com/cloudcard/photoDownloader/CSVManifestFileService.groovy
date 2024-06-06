package com.cloudcard.photoDownloader

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import org.apache.commons.csv.QuoteMode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import java.text.SimpleDateFormat

@Service
@ConditionalOnProperty(name = "ManifestFileService", havingValue = "CSVManifestFileService", matchIfMissing = false)
class CSVManifestFileService implements ManifestFileService {

    private static final Logger log = LoggerFactory.getLogger(CSVManifestFileService.class)

    @Value('${CSVManifestFileService.fileName:}')
    String fileName

    @Value('${CSVManifestFileService.fileNameDateFormat:}')
    String fileNameDateFormat

    @Value('${CSVManifestFileService.directory:}')
    String directory

    @Value('${CSVManifestFileService.delimiter:}')
    Character delimiter

    @Value('${CSVManifestFileService.quoteMode:}')
    String quoteMode

    @Value('${CSVManifestFileService.quoteCharacter:}')
    Character quoteCharacter

    @Value('${CSVManifestFileService.escapeCharacter:}')
    Character escapeCharacter

    @Value('${CSVManifestFileService.dateFormat:}')
    String dateFormat

    @Value('#{${CSVManifestFileService.headerAndColumnMap:}}')
    Map<String,String> headerAndColumnMap

    @PostConstruct
    void init() {

        log.info("CSV Manifest File Service fileName          : $fileName")
        log.info("CSV Manifest File Service fileNameDateFormat: $fileNameDateFormat")
        log.info("CSV Manifest File Service directory         : $directory")
        log.info("CSV Manifest File Service delimiter         : $delimiter")
        log.info("CSV Manifest File Service quoteMode         : $quoteMode")
        log.info("CSV Manifest File Service quoteCharacter    : $quoteCharacter")
        log.info("CSV Manifest File Service escapeCharacter   : $escapeCharacter")
        log.info("CSV Manifest File Service dateFormat        : $dateFormat")
        log.info("CSV Manifest File Service headerAndColumnMap: $headerAndColumnMap")

    }

    void createManifestFile(List<Photo> photosToDownload, List<PhotoFile> photoFiles) {

        if (!photosToDownload) { return }

        log.info("==========  Generating Manifest File  ==========")

        String filePath = "${directory}/${resolveFileName()}"

        FileWriter fileWriter = new FileWriter(new File(filePath))

        CSVFormat csvFormat = CSVFormat.DEFAULT
                                       .withDelimiter(delimiter)
                                       .withEscape(escapeCharacter)
                                       .withQuote(quoteCharacter).withQuoteMode(QuoteMode.valueOf(quoteMode))
                                       .withHeader(headerAndColumnMap.keySet() as String[])

        CSVPrinter csvPrinter = new CSVPrinter(fileWriter, csvFormat)

        photosToDownload.each { photo ->
            if (photoFiles.collect { it.photoId }.contains(photo.id)) {

                PhotoFile file = photoFiles.find {it.photoId == photo.id}

                List record = headerAndColumnMap.values().collect { column -> resolveValue(column, photo, file) }

                csvPrinter.printRecord(record)
            }
        }

        csvPrinter.flush()
        csvPrinter.close()
        fileWriter.close()

        log.info("Writing manifest file  to '${filePath}'")

        log.info("==========  Manifest File Generated  ==========")
    }

    def resolveValue(String column, Photo photo, PhotoFile file) {
        if (column.startsWith('static_')) { return column.split('_', 2)[1] }

        if (column == 'photo_fullFilePath') { return file.fileName }

        if (column == 'photo_fileName') { return "${file.baseName}.jpg" }

        def currentObject = photo

        for (property in column.split('\\.')) {
            try {
                currentObject = currentObject."$property"
            } catch (MissingPropertyException e) {
                return null
            }
        }

        if (currentObject instanceof Date && dateFormat) { return formatDate(currentObject) }

        currentObject
    }

    String formatDate(Date dateString) {
        new SimpleDateFormat(dateFormat).format(dateString)
    }

    String resolveFileName() {
        if (fileNameDateFormat) {
            return "${fileName}${new SimpleDateFormat(fileNameDateFormat).format(new Date())}.csv"
        }
        return "${fileName}.csv"
    }
}
