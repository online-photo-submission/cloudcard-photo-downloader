//package com.cloudcard.photoDownloader;
//
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.stereotype.Service;
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//        import java.util.Collection;
//import java.util.Date;
//import java.util.List;
//
//@Service
//public class CsvFileStorageService extends FileStorageService implements StorageService {
//
//    private static final Logger log = LoggerFactory.getLogger(CsvFileStorageService.class);
//
//    @Value("${downloader.enableCsv}")
//    private boolean enableCsv;
//
//    @Value("${downloader.csvDirectory}")
//    String csvDirectory;
//
//    @Value("${downloader.csvFilePrefix}")
//    private String csvFilePrefix;
//
//    @Value("${downloader.csvFileExtension}")
//    private String csvFileExtension;
//
//    @Value("${downloader.csvBatchIdDateFormat}")
//    private String csvBatchIdDateFormat;
//
//
//@PostConstruct
//    void init() {
//
//        throwIfBlank(csvDirectory, "The CSV Directory must be specified.");
//        throwIfBlank(csvFilePrefix, "The CSV File Prefix must be specified.");
//        throwIfBlank(csvFileExtension, "The CSV File Extension must be specified.");
//        throwIfBlank(csvBatchIdDateFormat, "The Batch ID Date Format must be specified.");
//
//        }
//
//    /*************************************************************************************************/
//
//    public List<PhotoFile> save(Collection<Photo> photos) throws Exception {
//
//     List<PhotoFile> photoFiles = super.save(photos);
//
//     if (enableCsv) createCsvFile(photoFiles);
//        return photoFiles;
//
//     }
//
//     private void createCsvFile(List<PhotoFile> photoFiles) throws IOException {
//
//     if (photoFiles == null || photoFiles.isEmpty())
//         return;
//
//     List<String> lines = new ArrayList<>();
//     String blankLine = "!";
//
//     lines.addAll(generateCsv(photoFiles));
//     lines.add(blankLine);
//
//
//     writeCsvFile(lines);
//     }
//
//    private Collection<? extends String> generateCsv(List<PhotoFile> photoFiles) {
//
//        return null;
//    }
//
//    private void writeCsvFile(List<String> lines) throws IOException {
//
//        log.info("Writing the CSV file...");
//        String fileName = csvDirectory + File.separator + csvFilePrefix
//                + new SimpleDateFormat(csvBatchIdDateFormat).format(new Date()) + csvFileExtension;
//        FileWriter writer = new FileWriter(fileName);
//        for (String line : lines) {
//            log.info(line);
//            writer.write(line + ",");
//        }
//        writer.write("\n");
//        writer.close();
//        log.info("...CSV file writing complete");
//    }
//
//
//}