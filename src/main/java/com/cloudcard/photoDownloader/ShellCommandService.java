package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ShellCommandService {

    private static final Logger log = LoggerFactory.getLogger(ShellCommandService.class);

    @Value("${ShellCommandService.preExecuteCommand:}")
    public String preExecuteCommand;

    @Value("${ShellCommandService.preDownloadCommand:}")
    public String preDownloadCommand;

    @Value("${ShellCommandService.postDownloadCommand:}")
    public String postDownloadCommand;

    @Value("${ShellCommandService.postExecuteCommand:}")
    public String postExecuteCommand;

    @Autowired
    private ShellCommandRunner shellCommandRunner;

    public boolean preExecute() {

        return run("Pre Execute", preExecuteCommand);
    }

    public boolean preDownload(List<Photo> photos) {

        if (photos == null || photos.isEmpty()) return true;
        return run("Pre Download", preDownloadCommand);
    }

    public boolean postDownload(List<PhotoFile> photoFiles) {

        if (photoFiles == null || photoFiles.isEmpty()) return true;
        return run("Post Download", postDownloadCommand);
    }

    public boolean postExecute() {

        return run("Post Execute", postExecuteCommand);
    }

    /* *** PRIVATE HELPERS *** */
    private boolean run(String description, String command) {

        if (command == null || command.isEmpty()) return true;
        log.info("Running " + description + " command: " + command);
        return shellCommandRunner.run(command);
    }
}
