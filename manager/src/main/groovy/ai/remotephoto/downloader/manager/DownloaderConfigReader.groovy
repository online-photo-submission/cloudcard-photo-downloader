package ai.remotephoto.downloader.manager

import java.nio.file.Files
import java.nio.file.Path

@Deprecated
class DownloaderConfigReader {

    Properties read(Path installDirectory) {
        Path propertiesFile = installDirectory.resolve('application.properties')

        Properties properties = new Properties()

        if (!Files.exists(propertiesFile)) {
            return properties
        }

        Files.newInputStream(propertiesFile).withCloseable { input ->
            properties.load(input)
        }

        return properties
    }
}
