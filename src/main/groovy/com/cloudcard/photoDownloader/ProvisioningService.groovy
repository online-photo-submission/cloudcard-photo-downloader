package com.cloudcard.photoDownloader

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProvisioningService {

    @Autowired
    OrigoService origoService

    @Autowired
    CloudCardService cloudCardService

    @PostConstruct
    init() {
//        origoService.ge
    }
}
