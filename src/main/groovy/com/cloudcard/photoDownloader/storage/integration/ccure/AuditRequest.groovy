package com.cloudcard.photoDownloader.storage.integration.ccure

class AuditRequest {
    String startDateTime
    String endDateTime
    int pageSize
    int pageNumber
    String objectType = CCurePersonnel.TYPE
    String[] messageTypes = ["ObjectCreated"]
    String sortOrder = "ServerUTC ASC"
    String objectGuid = ""
    int partitionId = 1

    AuditRequest(String startDateTime, String endDateTime, int pageSize, int pageNumber) {
        this.startDateTime = startDateTime
        this.endDateTime = endDateTime
        this.pageSize = pageSize
        this.pageNumber = pageNumber
    }
}