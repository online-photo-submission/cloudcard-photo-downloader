package com.cloudcard.photoDownloader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;

public class FileServiceTest {

    Random random = new Random();
    FileService fileService;
    final String fileName = "test-summary-file.log";

    @Before
    public void before() throws Exception {

        fileService = new FileService();
        Files.deleteIfExists(Paths.get(fileName));
    }

    @After
    public void after() throws Exception {

        Files.deleteIfExists(Paths.get(fileName));
    }

    @Test
    public void testWriteFile_EmptyFile() throws Exception {

        //setup
        List<String> lines = generateLines();

        //test writing
        fileService.writeFile(lines, fileName);

        //verify
        checkFileOutput(lines, 1);
    }

    @Test
    public void testWriteFile_Append() throws Exception {

        //setup
        List<String> lines = generateLines();

        //test writing
        fileService.writeFile(lines, fileName);
        // append more
        fileService.writeFile(lines, fileName);

        //verify
        checkFileOutput(lines, 2);
    }

    private List<String> generateLines() {

        List<String> lines = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            lines.add("test line " + random.nextInt());
        }
        return lines;
    }

    private void checkFileOutput(List<String> lines, int times) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        for (int i = 0; i < times; i++) {
            for (String line : lines) {
                assertThat(reader.readLine()).isEqualTo(line);
            }
        }
        assertThat(reader.readLine()).isNullOrEmpty();
    }
}