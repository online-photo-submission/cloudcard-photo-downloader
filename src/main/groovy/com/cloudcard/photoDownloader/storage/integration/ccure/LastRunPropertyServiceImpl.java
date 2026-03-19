package com.cloudcard.photoDownloader.storage.integration.ccure;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@Service
public class LastRunPropertyServiceImpl implements LastRunPropertyService {

    private final String TIMESTAMP_KEY = "last.run";

    @Value("${downloader.integration.properties:app-runtime.properties}")
    private String FILE_PATH;

    @Value("${downloader.integration.useLocalTime:false}")
    private boolean useLocalTime;

    public static final DateTimeFormatter CCURE_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");

    @Override
    public String getLastRunTimestamp() {
        Properties props = loadProperties();
        String value = props.getProperty(TIMESTAMP_KEY);

        if (value == null) {
            value = updateLastRunTimestamp();
        }
        return value;
    }

    @Override
    public String getCurrentTimestamp() {
        String formattedDate;

        if (useLocalTime) {
            // Capture system local time
            formattedDate = LocalDateTime.now().format(CCURE_DATE_FORMATTER);
        } else {
            // Capture UTC time specifically
            formattedDate = ZonedDateTime.now(ZoneId.of("UTC")).format(CCURE_DATE_FORMATTER);
        }

        return formattedDate;
    }

    @Override
    public String formatTimestamp(String utcTimestamp) {
        String formattedDate;

        if (useLocalTime) {
            ZoneId localZone = ZoneId.of("America/New_York");
            formattedDate = ZonedDateTime.parse(utcTimestamp).withZoneSameInstant(localZone).format(CCURE_DATE_FORMATTER);
        } else {
            formattedDate = ZonedDateTime.parse(utcTimestamp).format(CCURE_DATE_FORMATTER);
        }

        return formattedDate;
    }

    @Override
    public String updateLastRunTimestamp(String timestamp) {
        Properties props = loadProperties();
        props.setProperty(TIMESTAMP_KEY, timestamp);

        saveProperties(props);
        return timestamp;
    }

    @Override
    public String updateLastRunTimestamp() {
        return updateLastRunTimestamp(getCurrentTimestamp());
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        File file = new File(FILE_PATH);

        if (file.exists()) {
            try (InputStream is = new FileInputStream(file)) {
                props.load(is);
            } catch (IOException e) {
                throw new RuntimeException("Failed to load properties file", e);
            }
        }
        return props;
    }

    private void saveProperties(Properties props) {
        try {
            Path path = Paths.get(FILE_PATH);
            // Ensure the directory exists
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (OutputStream os = new FileOutputStream(FILE_PATH)) {
                props.store(os, "Runtime state - Do not manually edit unless instructed to do so");
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to properties file", e);
        }
    }
}