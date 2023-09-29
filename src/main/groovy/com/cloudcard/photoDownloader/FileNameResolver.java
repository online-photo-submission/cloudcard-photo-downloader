package com.cloudcard.photoDownloader;

public interface FileNameResolver {

    String getBaseName(Photo photo);

}
