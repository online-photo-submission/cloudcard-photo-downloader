package com.cloudcard.photoDownloader;

import java.util.List;

public interface PhotoService {
    List<Photo> fetchReadyForDownload() throws Exception;

    Photo markAsDownloaded(PhotoFile photo) throws Exception;

    long minDownloaderDelay();
}
