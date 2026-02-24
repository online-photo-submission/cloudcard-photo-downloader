package com.cloudcard.photoDownloader;

import java.util.List;

public interface PhotoService {
    List<Photo> fetchReadyForDownload() throws Exception;

    Photo markAsDownloaded(Photo photo) throws Exception;

    long minDownloaderDelay();

    /**
     * implement this method to ensure all connections, sessions, and temporarily credentials are closed or terminated or logged out when the downloader loop is done.
     */
    void close();
}
