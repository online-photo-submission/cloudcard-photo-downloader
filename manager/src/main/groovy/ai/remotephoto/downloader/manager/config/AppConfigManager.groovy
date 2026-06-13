package ai.remotephoto.downloader.manager.config

import java.nio.file.Path

class AppConfigManager {

    static final Path APP_HOME = resolveAppHome()
    private final DownloaderConfigService downloaderConfigService = new DownloaderConfigService()
    Properties properties

    AppConfigManager() {
        this.properties = downloaderConfigService.loadProperties(APP_HOME)
    }

    Boolean getUseRemoteConfigs() {
        properties?.getProperty('downloader.useRemoteConfigs', 'true')?.toBoolean()
    }

    String getApiUrl() {
        properties?.get('cloudcard.api.url') as String ?: 'https://api.cloudcard.us/api'
    }

    String getPersistentAccessToken() {
        properties?.get('cloudcard.api.accessToken') as String ?: ''
    }

    String getIntegrationName() {
        properties?.get('cloudcard.integration.name') as String ?: 'Downloader'
    }

    String getAdditionalPropertiesText() {
        properties
            .findAll { key, value -> !DownloaderConfigService.MANAGED_KEYS.contains(key.toString()) }
            .collect { key, value -> "${key}=${value}" }
            .sort()
            .join(System.lineSeparator())
    }

    void saveConfiguration(String apiUrl, String persistentAccessToken, String integrationName, Boolean useRemoteConfig, String additionalPropertiesText) {
        downloaderConfigService.writeOrUpdate(
            APP_HOME,
            apiUrl,
            persistentAccessToken,
            integrationName,
            useRemoteConfig,
            additionalPropertiesText
        )
        // Reload properties after saving
        this.properties = downloaderConfigService.loadProperties(APP_HOME)
    }

    private static Path resolveAppHome() {
        String configuredAppHome = System.getProperty('app.home')

        if (configuredAppHome?.trim()) {
            return Path.of(configuredAppHome)
                       .toAbsolutePath()
                       .normalize()
        }

        return Path.of(System.getProperty('user.dir'))
                   .toAbsolutePath()
                   .normalize()
    }
}
