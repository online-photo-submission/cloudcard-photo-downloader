package com.cloudcard.photoDownloader;

import java.util.List;

public interface PhotoService {
    List<Photo> fetchReadyForDownload() throws Exception;

    Photo markAsDownloaded(Photo photo) throws Exception;

    long minDownloaderDelay();

    void markAsError(FailedPhotoFile failedPhotoFile);
}
