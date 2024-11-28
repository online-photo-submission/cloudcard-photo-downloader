package com.cloudcard.photoDownloader;

import java.util.Collection;
import java.util.List;

public interface StorageService {

    List<PhotoFile> save(Collection<Photo> photos) throws Exception;
    // Should filter out errors / nulls
}
