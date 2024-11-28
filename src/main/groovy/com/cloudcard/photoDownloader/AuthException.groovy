package com.cloudcard.photoDownloader

class AuthException extends Exception {

    public static final String MESSAGE = "Error while attempting to authenticate"

    AuthException() {

        super(MESSAGE);
    }

    AuthException(String message) {

        super("$message $MESSAGE")
    }
}
