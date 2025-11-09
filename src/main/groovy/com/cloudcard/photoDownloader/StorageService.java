package com.cloudcard.photoDownloader;

import java.util.Collection;
import java.util.List;

public interface StorageService {

    StorageResults save(Collection<Photo> photos) throws Exception;
}
