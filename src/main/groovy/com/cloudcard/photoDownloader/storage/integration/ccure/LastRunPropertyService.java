package com.cloudcard.photoDownloader.storage.integration.ccure;

public interface LastRunPropertyService {
    /**
     * Reads the timestamp. If the file/key doesn't exist, initializes it with the current time.
     */
    String getLastRunTimestamp();

    /**
     * Overwrites the timestamp property.
     *
     * @return The formatted timestamp string saved to the file.
     */
    String updateLastRunTimestamp();
}
