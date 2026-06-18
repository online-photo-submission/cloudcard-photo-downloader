package ai.remotephoto.downloader.manager.ui

import ai.remotephoto.downloader.manager.api.ApiUtil
import ai.remotephoto.downloader.manager.api.AuthenticationToken
import ai.remotephoto.downloader.manager.config.DownloaderConfigUtility
import ai.remotephoto.downloader.manager.servy.ServyConfigWriter
import ai.remotephoto.downloader.manager.servy.ServyServiceManager
import javafx.application.Platform
import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import javafx.concurrent.Task

import java.nio.file.Path

class MainViewModel {
    DownloaderConfigUtility configService = new DownloaderConfigUtility()

    // Form data fields
    final StringProperty apiUrl = new SimpleStringProperty()
    final StringProperty integrationName = new SimpleStringProperty()
    final StringProperty accessToken = new SimpleStringProperty()
    final BooleanProperty useRemoteConfigs = new SimpleBooleanProperty(true)
    final StringProperty advancedProperties = new SimpleStringProperty()

    // API connection states
    final StringProperty apiStatusText = new SimpleStringProperty('Unknown')
    final StringProperty apiStatusState = new SimpleStringProperty('UNKNOWN') // SUCCESS, ERROR, UNKNOWN

    // Busy Indicator States
    final BooleanProperty isProcessing = new SimpleBooleanProperty(false)
    final StringProperty processingMessage = new SimpleStringProperty('')

    final StringProperty logOutput = new SimpleStringProperty('')

    void log(String message) {
        runOnFxThread {
            logOutput.value += formatLogLine(message)
        }
    }

    void saveConfiguration(Path appHome) {
        runBackground('Applying configuration') {
            saveAndReloadConfiguration(appHome)
            Path json = ServyConfigWriter.write(appHome)
            String installOutput = ServyServiceManager.install(appHome, json)
            return "${installOutput}\nApplied configuration and installed downloader service."
        }
    }

    void startService(Path appHome) {
        runBackground('Starting downloader service') {
            String status = ServyServiceManager.refresh(appHome)

            if (status.toLowerCase().contains('running')) {
                log("Service was already running, so it will be restarted.")

                String stopOutput = ServyServiceManager.stop(appHome)
                String startOutput = ServyServiceManager.start(appHome)

                return  "Restarting... \n ${stopOutput} \n ${startOutput}"
            }

            return ServyServiceManager.start(appHome)
        }
    }

    void stopService(Path appHome) {
        runBackground('Stopping downloader service') {
            return ServyServiceManager.stop(appHome)
        }
    }

    void refreshServiceStatus(Path appHome) {
        runBackground('Refreshing service status') {
            return ServyServiceManager.refresh(appHome)
        }
    }

    void runBackground(String actionName, Closure<String> work) {
        runOnFxThread {
            isProcessing.set(true)
            processingMessage.set("${actionName}...")
        }

        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                return work.call()
            }
        }

        task.setOnSucceeded {
            isProcessing.set(false)

            if (task.value?.trim()) {
                log(task.value.trim())
            }
        }

        task.setOnFailed {
            isProcessing.set(false)
            log("${actionName} failed: ${task.exception?.message ?: 'Unknown error'}")
        }

        Thread worker = new Thread(task)
        worker.daemon = true
        worker.start()
    }

    void saveAndReloadConfiguration(Path appHome) {
        configService.writeOrUpdate(
            appHome,
            apiUrl.get(),
            accessToken.get(),
            integrationName.get(),
            useRemoteConfigs.get(),
            advancedProperties.get()
        )

        log("Saved ${appHome.resolve('application.properties')}")

        // Reload the properties so the user can see invalid ones were automatically removed.
        loadConfiguration(appHome)
    }

    void loadConfiguration(Path appHome) {
        Properties props = configService.loadProperties(appHome)


        Set<String> rules = DownloaderConfigUtility.MANAGED_KEYS

        String properties = props.findAll { key, val -> !rules.contains(key.toString()) }
            .collect { key, val -> "${key}=${val}" }
            .sort()
            .join(System.lineSeparator())

        // 1. Populate the first-class fields
        runOnFxThread {
            apiUrl.set(props.getProperty('cloudcard.api.url', 'https://api.cloudcard.us/api'))
            integrationName.set(props.getProperty('cloudcard.integration.name', 'Downloader'))
            accessToken.set(props.getProperty('cloudcard.api.accessToken', ''))
            useRemoteConfigs.set(props.getProperty('downloader.useRemoteConfigs', 'true').toBoolean())
            advancedProperties.set(properties)
        }
    }

    void testConnection() {
        runBackground('Testing API connection') {
            ApiUtil apiClient = new ApiUtil()

            try {
                AuthenticationToken token = apiClient.authenticate(accessToken.get(), apiUrl.get())

                runOnFxThread {
                    apiStatusText.set('API Connected')
                    apiStatusState.set('SUCCESS')
                }

                return "Successfully authenticated as ${token.username}!"
            } catch (Exception e) {
                runOnFxThread {
                    apiStatusText.set('Failed')
                    apiStatusState.set('ERROR')
                }

                return "ERROR: ${e.message}"
            }
        }
    }

    private static void runOnFxThread(Closure<?> action) {
        if (Platform.isFxApplicationThread()) {
            action.call()
        } else {
            Platform.runLater {
                action.call()
            }
        }
    }

    private static String formatLogLine(Object message) {
        "${new Date()}  ${message ?: ''}${System.lineSeparator()}"
    }
}
