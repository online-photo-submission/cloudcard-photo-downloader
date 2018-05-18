package com.cloudcard.photoDownloader;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class DownloaderService {

    private static final Logger log = LoggerFactory.getLogger(DownloaderService.class);

    @Value("${downloader.photoDirectory}")
    String photoDirectory;

    @Value("${downloader.udfDirectory}")
    String udfDirectory;

    @Value("${downloader.slash}")
    private String slash;

    @Value("${downloader.udfFilePrefix}")
    private String udfFilePrefix;

    @Value("${downloader.udfFileExtension}")
    private String udfFileExtension;

    @Value("${downloader.descriptionDateFormat}")
    private String descriptionDateFormat;

    @Value("${downloader.batchIdDateFormat}")
    private String batchIdDateFormat;

    @Value("${downloader.createdDateFormat}")
    private String createdDateFormat;

    @Value("${downloader.enableUdf}")
    private boolean enableUdf;

    @Autowired
    CloudCardPhotoService cloudCardPhotoService;

    @Scheduled(fixedDelayString = "${downloader.delay.milliseconds}")
    public void downloadPhotos() throws Exception {

        log.info("Downloading photos");

        List<PhotoFile> photoFiles = new ArrayList<>();
        for (Photo photo : cloudCardPhotoService.fetchApprovedPhotos()) {
            log.info("Downloading: " + photo.getPublicKey());
            PhotoFile photoFile = downloadPhotoFiles(photo);
            if (photoFile != null) photoFiles.add(photoFile);
        }
        log.info(photoFiles.size() + " photos downloaded.");

        if (enableUdf) createUdfFile(photoFiles);
    }

    public PhotoFile downloadPhotoFiles(Photo photo) throws Exception {

        String bytesURL = photo.getLinks().getBytes();
        String studentID = photo.getPerson().getIdentifier();

        if (studentID == null || studentID.isEmpty()) {
            log.error(photo.getPerson().getEmail() + " is missing an ID number, so photo " + photo.getId() + " cannot be downloaded.");
            return null;
        }

        HttpResponse<String> response = Unirest.get(bytesURL).header("accept", "image/jpeg;charset=utf-8").header("Content-Type", "image/jpeg;charset=utf-8").asString();

        if (response.getStatus() != 200) {
            log.error("Status " + response.getStatus() + "returned from CloudCard API when retrieving photos bytes.");
            return null;
        }

        String fileName = photoDirectory + slash + studentID + ".jpg";
        writeBytesToFile(fileName, getBytes(response));
        return new PhotoFile(studentID, fileName);
    }

    private void writeBytesToFile(String fileName, byte[] bytes) throws IOException {

        File file = new File(fileName);

        FileOutputStream outputStream = new FileOutputStream(file);

        if (!file.exists()) {
            file.createNewFile();
        }
        outputStream.write(bytes);
        outputStream.flush();
        outputStream.close();
    }


    private byte[] getBytes(HttpResponse<String> response) throws IOException {

        InputStream rawBody = response.getRawBody();
        byte[] bytes = new byte[ rawBody.available() ];
        rawBody.read(bytes);
        return bytes;
    }

    private void createUdfFile(List<PhotoFile> photoFiles) throws IOException {

        List<String> lines = new ArrayList<>();
        String blankLine = "!";

        lines.addAll(generateUdfHeader(photoFiles));
        lines.add(blankLine);
        lines.addAll(generateUdfFormat(photoFiles));
        lines.add(blankLine);
        lines.addAll(generateUdfData(photoFiles));

        writeUdfFile(lines);
    }

    private List<String> generateUdfHeader(List<PhotoFile> photoFiles) {

        List<String> header = new ArrayList<>();
        header.add("!Description: Photo Import " + new SimpleDateFormat(descriptionDateFormat).format(new Date()));
        header.add("!Source: CloudCard Online Photo Submission");
        header.add("!BatchID: " + new SimpleDateFormat(batchIdDateFormat).format(new Date()));
        header.add("!Created: " + new SimpleDateFormat(createdDateFormat).format(new Date()));
        header.add("!Version:");
        header.add("!RecordCount: " + photoFiles.size());
        return header;
    }

    private List<String> generateUdfFormat(List<PhotoFile> photoFiles) {

        List<String> format = new ArrayList<>();
        int longestId = findLongestId(photoFiles);
        int longestFilename = findLongestFilename(photoFiles);

        format.add("!BeginFormat");
        format.add("!Odyssey_PCS,\t1,\t" + longestId + "\t\"IDNUMBER\"");
        format.add("!Odyssey_PCS,\t" + (longestId + 1) + ",\t" + longestFilename + "\t\"PICTUREPATH\"");
        format.add("!EndFormat");
        return format;
    }

    private List<String> generateUdfData(List<PhotoFile> photoFiles) {

        List<String> data = new ArrayList<>();
        int longestId = findLongestId(photoFiles);

        data.add("!BeginData");
        for (PhotoFile photoFile : photoFiles) {
            data.add(fixedLengthString(photoFile.getIdNumber(), longestId) + photoFile.getFileName());
        }
        data.add("!EndData");
        return data;
    }

    private int findLongestFilename(List<PhotoFile> photoFiles) {

        int longestFilename = 0;
        for (PhotoFile photoFile : photoFiles) {
            longestFilename = Math.max(photoFile.getFileName().length(), longestFilename);
        }
        return longestFilename;
    }

    private int findLongestId(List<PhotoFile> photoFiles) {

        int longestId = 0;
        for (PhotoFile photoFile : photoFiles) {
            longestId = Math.max(photoFile.getIdNumber().length(), longestId);
        }
        return longestId;
    }

    private static String fixedLengthString(String string, int length) {

        return String.format("%1$-" + length + "s", string);
    }

    private void writeUdfFile(List<String> lines) throws IOException {

        log.info("Writing the UDF file...");
        String fileName = udfDirectory + slash + udfFilePrefix + new SimpleDateFormat(batchIdDateFormat).format(new Date()) + udfFileExtension;
        FileWriter writer = new FileWriter(fileName);
        for (String line : lines) {
            log.info(line);
            writer.write(line + "\n");
        }
        writer.close();
        log.info("...UDF file writing complete");
    }
}
