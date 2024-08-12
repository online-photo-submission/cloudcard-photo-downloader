package com.cloudcard.photoDownloader.integrations.origo

import com.cloudcard.photoDownloader.Person
import com.cloudcard.photoDownloader.integrations.PeopleService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(value = "downloader.storageService", havingValue = "OrigoStorageService")
class OrigoPeopleService extends OrigoService implements PeopleService {

    private static final Logger log = LoggerFactory.getLogger(OrigoPeopleService.class)

    List<Person> getPeople() {
        List<Object> peopleObjects = getEvents()
        List<Person> people = []

        for (person in peopleObjects) {
            person = person.data
            if (person.status != 'USER_CREATED') continue
            Person newPerson = new Person()

            newPerson.identifier = person.userId
            newPerson.email = person.email
            newPerson.username = "${person.firstName}-${person.lastName}"

            String serializedPerson = new ObjectMapper().writeValueAsString(newPerson)
            log.info("PERSON: ${serializedPerson}")
            people.add(newPerson)
        }

        return people
    }
}
