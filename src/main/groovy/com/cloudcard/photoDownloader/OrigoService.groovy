package com.cloudcard.photoDownloader

import org.springframework.beans.factory.annotation.Value

abstract class OrigoService {
    // Handles business logic / processing for incoming events and requests from API

    @Value('${Origo.filterSet}')
    private filterSet
    def compareExistingFilters() {
        // compares existing filters with those specified in Application.properties
    }
}
