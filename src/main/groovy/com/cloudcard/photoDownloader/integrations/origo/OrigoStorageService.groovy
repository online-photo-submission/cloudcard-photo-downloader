package com.cloudcard.photoDownloader.integrations.origo

import com.cloudcard.photoDownloader.Photo
import com.cloudcard.photoDownloader.PhotoFile
import com.cloudcard.photoDownloader.StorageService
import org.springframework.stereotype.Component

@Component
class OrigoStorageService implements StorageService{

    List<PhotoFile> save(Collection<Photo> photos) {
        return null
    }

}
