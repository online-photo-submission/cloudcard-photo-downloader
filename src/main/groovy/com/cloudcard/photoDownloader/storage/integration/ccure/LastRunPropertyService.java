package com.cloudcard.photoDownloader.storage.integration.ccure;

import java.time.LocalDateTime;

public interface LastRunPropertyService {
    /**
     * Reads the timestamp. If the file/key doesn't exist, initializes it with the current time.
     */
    String getLastRunTimestamp();

    /**
     * @return a formatted string of the current time.
     */
    String getCurrentTimestamp();

    /**
     * @return a formatted string of the provided UTC time.
     */
    String formatTimestamp(String utcTimestamp);

    /**
     * Overwrites the timestamp property.
     *
     * @return The formatted timestamp string saved to the file.
     */
    String updateLastRunTimestamp();

    /**
     * Overwrites the timestamp property.
     *
     * @param timestamp The formatted timestamp to save.
     * @return The formatted timestamp string saved to the file.
     */
    String updateLastRunTimestamp(String timestamp);
}
