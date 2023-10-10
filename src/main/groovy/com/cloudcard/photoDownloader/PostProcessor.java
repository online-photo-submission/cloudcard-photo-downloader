package com.cloudcard.photoDownloader;

public interface PostProcessor {

    PhotoFile process(Photo photo, String photoDirectory, PhotoFile photoFile);
}
