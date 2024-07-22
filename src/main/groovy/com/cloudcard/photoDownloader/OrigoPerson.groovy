package com.cloudcard.photoDownloader

import com.fasterxml.jackson.annotation.JsonProperty

class OrigoPerson extends Person {

    @JsonProperty("cardholderGroupName")
    String cardholderGroupName
}
