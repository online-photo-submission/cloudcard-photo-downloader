package com.cloudcard.photoDownloader;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(value = "downloader.summaryService", havingValue = "SimpleSummaryService", matchIfMissing = true)
public class SimpleSummaryService implements SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SimpleSummaryService.class);

    @Value("${SimpleSummaryService.fileName:}")
    private String fileName;

    @Value("${SimpleSummaryService.directory}")
    private String directory;

    @Autowired
    private FileUtil fileUtil;

    @PostConstruct
    public void init() {

        File directory = new File(this.directory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }

    @Override
    public void createSummary(List<Photo> photos, List<PhotoFile> photoFiles) throws Exception {

        if (photos.isEmpty()) return;

        int attempted = photos.size();
        int succeeded = photoFiles.size();
        int failed = photos.size() - photoFiles.size();
        String summary = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM-dd HH:mm")) + " | Attempted: " + pad(attempted) + "| Succeeded: " + pad(succeeded) + "| Failed: " + pad(failed);

        List<String> lines = new ArrayList<>();
        lines.add(summary);

        log.info("Writing summary to '" + getFileName() + "'\n" + summary);
        fileUtil.writeFile(lines, getFileName());
    }

    private String pad(int i) {

        return StringUtils.rightPad(i + "", 8);
    }

    private String getFileName() {

        return this.fileName.isEmpty() ? directory + "/cloudcard-download-summary_" + LocalDate.now() + ".txt" : directory + "/" + fileName;
    }

}
