package ai.remotephoto.downloader.manager.ui

import ai.remotephoto.downloader.manager.service.DownloaderConfigService
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

import java.nio.file.Path

class MainViewModel {

    // 1. Form Data Fields (Observable wrappers around your strings)
    final StringProperty apiUrl = new SimpleStringProperty()
    final StringProperty integrationName = new SimpleStringProperty()
    final StringProperty accessToken = new SimpleStringProperty()
    final BooleanProperty useRemoteConfigs = new SimpleBooleanProperty(true)
    final StringProperty advancedProperties = new SimpleStringProperty()

    // 2. Global Status UI States
    final StringProperty apiStatusText = new SimpleStringProperty('Unknown')
    final StringProperty apiStatusState = new SimpleStringProperty('UNKNOWN') // SUCCESS, ERROR, UNKNOWN

    // 3. Busy Indicator States
    final BooleanProperty isProcessing = new SimpleBooleanProperty(false)
    final StringProperty processingMessage = new SimpleStringProperty('')

    // 4. The Log Callback Pipeline
    // This connects the model's text back to the View's TextArea without the model knowing the view exists.
    Closure<Void> logConsumer = { String msg -> println "Fallback Log: ${msg}" }

    // Helper method inside the model to easily push logs back to the view
    void log(String message) {
        logConsumer.call(message)
    }

    void loadConfiguration(Path appHome, DownloaderConfigService configService) {
        Properties props = configService.loadProperties(appHome)

        // 1. Populate the first-class fields
        apiUrl.set(props.getProperty('cloudcard.api.url', 'https://api.cloudcard.us/api'))
        integrationName.set(props.getProperty('cloudcard.integration.name', 'Downloader'))
        accessToken.set(props.getProperty('cloudcard.api.accessToken', ''))
        useRemoteConfigs.set(props.getProperty('downloader.useRemoteConfigs', 'true').toBoolean())

        Set<String> rules = DownloaderConfigService.MANAGED_KEYS

        String overrides = props.findAll { key, val -> !rules.contains(key.toString()) }
            .collect { key, val -> "${key}=${val}" }
            .sort()
            .join(System.lineSeparator())

        advancedProperties.set(overrides)
    }
}