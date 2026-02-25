package com.cloudcard.photoDownloader;

import java.util.Collection;

public interface StorageService {

    StorageResults save(Collection<Photo> photos) throws Exception;
}
