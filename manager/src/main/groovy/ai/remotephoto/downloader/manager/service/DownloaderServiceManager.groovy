package ai.remotephoto.downloader.manager.service

import ai.remotephoto.downloader.manager.config.AppConfigManager
import javafx.concurrent.Task
import javafx.scene.control.Label
import javafx.scene.control.ProgressIndicator

import java.nio.file.Path

class DownloaderServiceManager {

    private final AppConfigManager appConfigManager
    private final ProgressIndicator busyIndicator
    private final Label busyLabel
    private final Closure<String> appendOutputCallback
    private final Closure<Void> saveConfigurationCallback // Callback to save configuration from MainView

    DownloaderServiceManager(AppConfigManager appConfigManager, ProgressIndicator busyIndicator, Label busyLabel, Closure<String> appendOutputCallback, Closure<Void> saveConfigurationCallback) {
        this.appConfigManager = appConfigManager
        this.busyIndicator = busyIndicator
        this.busyLabel = busyLabel
        this.appendOutputCallback = appendOutputCallback
        this.saveConfigurationCallback = saveConfigurationCallback
    }

    void applyConfiguration() {
        runBackground('Applying configuration') {
            saveConfigurationCallback.call() // Call back to MainView to save UI state
            Path json = ServyConfigWriter.write(AppConfigManager.APP_HOME)
            String installOutput = ServyServiceManager.install(AppConfigManager.APP_HOME, json)
            return "${installOutput}\nApplied configuration and installed CloudCardDownloader service."
        }
    }

    void startService() {
        runBackground('Starting service') {
            ServyServiceManager.start(AppConfigManager.APP_HOME)
        }
    }

    void stopService() {
        runBackground('Stopping service') {
            ServyServiceManager.stop(AppConfigManager.APP_HOME)
        }
    }

    void refreshServiceStatus() {
        runBackground('Refreshing service status') {
            ServyServiceManager.refresh(AppConfigManager.APP_HOME)
        }
    }

    private void runBackground(String actionName, Closure<String> work) {
        busyIndicator.visible = true
        busyIndicator.managed = true
        busyLabel.text = "${actionName}..."
        busyLabel.visible = true
        busyLabel.managed = true

        Task<String> task = new Task<String>() {
            @Override
            protected String call() {
                return work.call()
            }
        }

        task.setOnSucceeded {
            busyIndicator.visible = false
            busyIndicator.managed = false
            busyLabel.visible = false
            busyLabel.managed = false

            if (task.value?.trim()) {
                appendOutputCallback.call(task.value.trim())
            }
        }

        task.setOnFailed {
            busyIndicator.visible = false
            busyIndicator.managed = false
            busyLabel.visible = false
            busyLabel.managed = false

            Throwable exception = task.exception
            appendOutputCallback.call("${actionName} failed: ${exception?.message ?: 'Unknown error'}")
        }

        Thread worker = new Thread(task)
        worker.daemon = true
        worker.start()
    }
}
