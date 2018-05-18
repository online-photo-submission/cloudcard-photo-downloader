package com.cloudcard.photoDownloader;

import java.util.Collection;

public interface StorageService {

    void save(Collection<Photo> photos) throws Exception;
}
