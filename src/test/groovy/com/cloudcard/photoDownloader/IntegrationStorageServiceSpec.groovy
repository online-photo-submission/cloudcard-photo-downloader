package com.cloudcard.photoDownloader

import spock.lang.*

class IntegrationStorageServiceSpec extends Specification {

    IntegrationStorageService service = new IntegrationStorageService()

    byte[] photoBytes = generateByteArray(0)

    void setup() {
        service.integrationStorageClient = Mock(IntegrationStorageClient)
        service.fileNameResolver = Mock(FileNameResolver)
    }

    void "test save with no photos"() {
        given:
        List<Photo> photos = [];

        when:
        StorageResults storageResults = service.save(photos);

        then:
        storageResults.downloadedPhotoFiles.size() == 0;

        0 * service.integrationStorageClient._
    }

    void "test save with one good photo"() {
        given:
        String identifier = "100012345"
        Photo photo = new Photo(
            id: 1,
            person: new Person(identifier: identifier, email: "$identifier@bacon.edu"),
            bytes: photoBytes
        )
        List<Photo> photos = [photo]

        when:
        StorageResults storageResults = service.save(photos)

        then:
        storageResults.downloadedPhotoFiles.size() == 1
        assert storageResults.downloadedPhotoFiles[0]
        storageResults.downloadedPhotoFiles[0].baseName == identifier
        storageResults.downloadedPhotoFiles[0].photoId == 1

        1 * service.fileNameResolver.getBaseName(photo) >> identifier
        1 * service.integrationStorageClient.putPhoto(identifier, photoBytes)
    }

    void "test save with three good photos"() {
        given:
        List<String> identifiers = ["100012345", "100012346", "100012347"]
        List<Photo> photos = identifiers.withIndex().collect { it, index ->
            new Photo(
                id: index,
                person: new Person(identifier: it, email: "$it@bacon.edu"),
                bytes: generateByteArray(index)
            )
        }

        when:
        StorageResults storageResults = service.save(photos)

        then:
        storageResults.downloadedPhotoFiles.size() == 3
        storageResults.downloadedPhotoFiles.eachWithIndex { it, index ->
            assert it
            assert it.photoId == index
            assert it.baseName == identifiers[index]
        }

        1 * service.fileNameResolver.getBaseName(photos[0]) >> identifiers[0]
        1 * service.fileNameResolver.getBaseName(photos[1]) >> identifiers[1]
        1 * service.fileNameResolver.getBaseName(photos[2]) >> identifiers[2]
        1 * service.integrationStorageClient.putPhoto(identifiers[0], generateByteArray(0))
        1 * service.integrationStorageClient.putPhoto(identifiers[1], generateByteArray(1))
        1 * service.integrationStorageClient.putPhoto(identifiers[2], generateByteArray(2))
    }

    void "test save with one runtime failure, one persistent failure, and one successful photo"() {
        given:
        List<String> identifiers = ["100012345", "bad012346", "100012347"]
        List<Photo> photos = identifiers.withIndex().collect { it, index ->
            new Photo(
                id: index,
                person: new Person(identifier: it, email: "$it@bacon.edu"),
                bytes: generateByteArray(index)
            )
        }

        when:
        StorageResults storageResults = service.save(photos)

        then: "downloadedPhotoFiles contains only the first photo"
        storageResults.downloadedPhotoFiles.size() == 1
        assert storageResults.downloadedPhotoFiles[0]
        storageResults.downloadedPhotoFiles[0].baseName == identifiers[0]
        storageResults.downloadedPhotoFiles[0].photoId == 0

        and: "failedPhotoFiles contains only the last photo"
        storageResults.failedPhotoFiles.size() == 1
        assert storageResults.failedPhotoFiles[0]
        storageResults.failedPhotoFiles[0].photoId == 2
        storageResults.failedPhotoFiles[0].errorMessage == "Failed to upload photo"

        and: "mock invocations"
        1 * service.fileNameResolver.getBaseName(photos[0]) >> identifiers[0]
        1 * service.fileNameResolver.getBaseName(photos[1]) >> identifiers[1]
        1 * service.fileNameResolver.getBaseName(photos[2]) >> identifiers[2]
        1 * service.integrationStorageClient.putPhoto(identifiers[0], generateByteArray(0))
        1 * service.integrationStorageClient.putPhoto(identifiers[1], generateByteArray(1)) >> {
            throw new RuntimeException("Failed to upload photo")
        }
        1 * service.integrationStorageClient.putPhoto(identifiers[2], generateByteArray(2)) >> {
            throw new FailedPhotoFileException("Failed to upload photo")
        }
    }

    void "test save when putPhoto fails with a runtime exception"() {
        given:
        String identifier = "100012345"
        Photo photo = new Photo(
            id: 1,
            person: new Person(identifier: identifier, email: "$identifier@bacon.edu"),
            bytes: photoBytes
        )
        List<Photo> photos = [photo]

        when:
        StorageResults storageResults = service.save(photos)

        then:
        storageResults.downloadedPhotoFiles.size() == 0

        1 * service.fileNameResolver.getBaseName(photo) >> identifier
        1 * service.integrationStorageClient.putPhoto(identifier, photoBytes) >> {
            throw new RuntimeException("Failed to upload photo")
        }
    }

    void "test save when putPhoto fails with a failed photo file exception"() {
        given:
        String identifier = "100012345"
        Photo photo = new Photo(
            id: 1,
            person: new Person(identifier: identifier, email: "$identifier@bacon.edu"),
            bytes: photoBytes
        )
        List<Photo> photos = [photo]

        when:
        StorageResults storageResults = service.save(photos)

        then:
        storageResults.downloadedPhotoFiles.size() == 0
        storageResults.failedPhotoFiles.size() == 1
        assert storageResults.failedPhotoFiles[0]
        storageResults.failedPhotoFiles[0].photoId == 1
        storageResults.failedPhotoFiles[0].errorMessage == "Failed to upload photo"


        1 * service.fileNameResolver.getBaseName(photo) >> identifier
        1 * service.integrationStorageClient.putPhoto(identifier, photoBytes) >> {
            throw new FailedPhotoFileException("Failed to upload photo")
        }
    }

    void "test save when accountId not resolvable"() {
        given:
        Photo photo = new Photo(
            id: 1,
            person: new Person(identifier: null, email: "null@bacon.edu"),
            bytes: photoBytes
        )

        when:
        PhotoFile photoFile = service.save(photo)

        then:
        photoFile == null

        1 * service.fileNameResolver.getBaseName(photo) >> null
        0 * service.integrationStorageClient.putPhoto(_, _)
    }

    void "test save with photo missing bytes"() {
        given:
        String identifier = "100012345"
        Photo photo = new Photo(
            id: 1,
            person: new Person(identifier: identifier, email: "$identifier@bacon.edu"),
            bytes: null
        )

        when:
        PhotoFile photoFile = service.save(photo)

        then:
        photoFile == null

        1 * service.fileNameResolver.getBaseName(photo) >> identifier
        0 * service.integrationStorageClient.putPhoto(_, _)
    }

    void "test save with photo missing person"() {
        given:
        Photo photo = new Photo(
            id: 1,
            person: null,
            bytes: null
        )

        when:
        PhotoFile photoFile = service.save(photo)

        then:
        photoFile == null

        0 * service.fileNameResolver.getBaseName(photo)
        0 * service.integrationStorageClient.putPhoto(_, _)
    }

    /*** Private Helpers ***/
    static byte[] generateByteArray(int index) {
        (index..(index+5)) as byte[]
    }
}
