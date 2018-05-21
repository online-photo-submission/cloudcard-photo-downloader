package com.cloudcard.photoDownloader;

public class ApplicationPropertiesException extends RuntimeException {

    public static final String MESSAGE = "Please update the 'application.properties' file.";

    ApplicationPropertiesException() {

        super(MESSAGE);
    }

    ApplicationPropertiesException(String message) {

        super(message + " " + MESSAGE);
    }
}
