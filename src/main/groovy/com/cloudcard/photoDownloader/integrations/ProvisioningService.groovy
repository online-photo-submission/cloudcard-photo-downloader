package com.cloudcard.photoDownloader.integrations

import com.cloudcard.photoDownloader.Person
import com.cloudcard.photoDownloader.TokenService
import jakarta.annotation.PostConstruct
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProvisioningService {
    private static final Logger log = LoggerFactory.getLogger(ProvisioningService.class)


    @Autowired
    PeopleService peopleService

    @Autowired
    CloudCardService cloudCardService

    @Autowired
    TokenService tokenService

    @PostConstruct
    init() {
//        tokenService.login()
//        cloudCardService.
        List<Person> people = peopleService.getPeople()

        for (person in people) {
            cloudCardService.provisionPerson(person)
        }
//        tokenService.logout()
    }
}
