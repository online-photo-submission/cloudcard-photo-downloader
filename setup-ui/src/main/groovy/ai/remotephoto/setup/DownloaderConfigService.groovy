package ai.remotephoto.setup

import java.nio.file.Files
import java.nio.file.Path

class DownloaderConfigService {

    private static final String PROPERTIES_FILE_NAME = 'application.properties'

    private static final Set<String> MANAGED_KEYS = [
        'cloudcard.api.url',
        'cloudcard.api.accessToken',
        'cloudcard.integrationName',
        'downloader.useRemoteConfigs'
    ] as Set

    Properties props = new Properties()

    void writeOrUpdate(Path installDirectory, String apiUrl, String persistentAccessToken, String integrationName, Boolean useRemoteConfig, String additionalPropertiesText) {
        Files.createDirectories(installDirectory)
        Path propertiesFile = installDirectory.resolve(PROPERTIES_FILE_NAME)

        props = loadProperties(installDirectory)

        put(props, 'cloudcard.api.url', apiUrl)
        put(props, 'cloudcard.api.accessToken', persistentAccessToken)
        put(props, 'cloudcard.integrationName', integrationName)
        put(props, 'downloader.useRemoteConfigs', useRemoteConfig?.toString())

        removeAdditionalProperties(props)
        putAdditionalProperties(props, additionalPropertiesText)

        Files.newOutputStream(propertiesFile).withCloseable { output ->
            props.store(output, 'RemotePhoto Downloader configuration')
        }

    }

    Properties loadProperties(Path installDirectory) {
        Files.createDirectories(installDirectory)
        Path propertiesFile = installDirectory.resolve(PROPERTIES_FILE_NAME)

        if (Files.exists(propertiesFile)) {
            Files.newInputStream(propertiesFile).withCloseable { input ->
                props.load(input)
            }
        }

        return props
    }

    private static void put(Properties props, String key, String value) {
        if (value?.trim()) {
            props.setProperty(key, value.trim())
        }
    }

    private static void removeAdditionalProperties(Properties props) {
        props.keySet()
            .collect { it.toString() }
            .findAll { key -> !MANAGED_KEYS.contains(key) }
            .each { key -> props.remove(key) }
    }

    private static void putAdditionalProperties(Properties props, String additionalPropertiesText) {
        additionalPropertiesText
            ?.readLines()
            ?.collect { it.trim() }
            ?.findAll { line -> line && !line.startsWith('#') && line.contains('=') }
            ?.each { line ->
                String[] parts = line.split('=', 2)
                String key = parts[0].trim()
                String value = parts.length > 1 ? parts[1].trim() : ''

                if (!MANAGED_KEYS.contains(key)) {
                    put(props, key, value)
                }
            }
    }

}
