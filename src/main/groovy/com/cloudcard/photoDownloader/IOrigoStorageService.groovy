package com.cloudcard.photoDownloader

interface IOrigoStorageService {
    List<Object> parse(List<Object> items)

    def configure(List<Object> items)

    def store(List<Object> items)
}
