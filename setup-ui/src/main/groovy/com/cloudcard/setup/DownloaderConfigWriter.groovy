package com.cloudcard.setup

import java.nio.file.Files
import java.nio.file.Path

class DownloaderConfigWriter {

    static void write(Path installDirectory, String apiUrl, String persistentAccessToken, String integrationName, Boolean useRemoteConfig) {
        Files.createDirectories(installDirectory)

        Path propertiesFile = installDirectory.resolve('application.properties')

        String content = """
cloudcard.api.url=$apiUrl
cloudcard.api.accessToken=$persistentAccessToken
cloudcard.integrationName=$integrationName
downloader.useRemoteConfigs=$useRemoteConfig""".trim()

        Files.writeString(propertiesFile, content)
    }
}
