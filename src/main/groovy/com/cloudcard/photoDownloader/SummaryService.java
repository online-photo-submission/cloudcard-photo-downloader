package com.cloudcard.photoDownloader;

import java.util.List;

public interface SummaryService {

    void createSummary(List<Photo> photos, List<PhotoFile> photoFiles) throws Exception;
}
