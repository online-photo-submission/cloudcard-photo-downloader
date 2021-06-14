package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

@Component
public class FileUtil {

    static final Logger log = LoggerFactory.getLogger(FileUtil.class);

    public String writeBytesToFile(String directoryName, String fileName, byte[] bytes) throws IOException {

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

    public void writeFile(List<String> lines, String fileName) throws IOException {

        log.debug("Writing the file: " + fileName);
        FileWriter writer = new FileWriter(fileName, true);
        for (String line : lines) {
            log.debug(line);
            writer.write(line + "\n");
        }
        writer.close();
        log.debug("File writing complete.");
    }
}