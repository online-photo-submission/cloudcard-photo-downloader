package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class OrigoService {
    // Handles business logic / processing for incoming events and requests from API

    @Autowired
    OrigoClient client

    @PostConstruct
    init() {}
}
