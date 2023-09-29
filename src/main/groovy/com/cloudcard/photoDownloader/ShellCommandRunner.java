package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Component
public class ShellCommandRunner {

    private static final Logger log = LoggerFactory.getLogger(ShellCommandRunner.class);

    public boolean run(String command) {

        ProcessBuilder processBuilder = new ProcessBuilder();

        processBuilder.command(command);

        try {

            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                log.info(line);
            }

            int exitCode = process.waitFor();
            if (exitCode != 0)
                log.error(command + " exited with error code: " + exitCode);
            return exitCode == 0;

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return false;
    }
}