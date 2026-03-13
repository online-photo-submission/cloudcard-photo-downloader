package com.cloudcard.photoDownloader

interface IntegrationStorageClient {

    /**
     * Returns the name of the target system for identification in logs and error messages.
     * @return
     */
    String getSystemName()

    /**
     * Uploads a photo to the target system.
     * This method is expected to abstract away any specifics of dealing with the target system's API,
     * such as authentication, session management, and data format.
     * If persistent sessions are necessary, the implementation SHOULD create a session the first time this method is called,
     * and reuse it for subsequent calls.
     *
     * This method is expected to throw exceptions if the upload fails for any reason and return otherwise.
     *
     * @param identifier - account identifier in the target system
     * @param photo - the complete photo object so that the client can retrieve bytes, person info, etc.
     * @return
     */
    void putPhoto(String identifier, Photo photo)

    /**
     * Closes any persistent session or connection to the target system.
     * This method may be called by upstream code when it is done using the client for a session.
     * The implementation does not need to do anything if no session was created.
     * This method should also not fail for any reason, even if the session was never created or has already been closed.
     *
     * @return
     */
    void close()
}
