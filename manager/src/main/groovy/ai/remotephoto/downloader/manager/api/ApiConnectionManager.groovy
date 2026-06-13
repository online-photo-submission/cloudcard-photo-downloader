package ai.remotephoto.downloader.manager.api

import ai.remotephoto.downloader.manager.api.ApiUtil
import ai.remotephoto.downloader.manager.api.AuthenticationToken
import javafx.scene.shape.Circle
import javafx.scene.control.Label

class ApiConnectionManager {

    private final ApiUtil apiClient = new ApiUtil()
    private final Label serviceStatusLabel
    private final Circle apiStatusIndicator
    private final Closure<String> appendOutputCallback

    ApiConnectionManager(Label serviceStatusLabel, Circle apiStatusIndicator, Closure<String> appendOutputCallback) {
        this.serviceStatusLabel = serviceStatusLabel
        this.apiStatusIndicator = apiStatusIndicator
        this.appendOutputCallback = appendOutputCallback
    }

    void testConnection(String pat, String apiUrl) {
        try {
            AuthenticationToken token = apiClient.authenticate(pat, apiUrl)
            appendOutputCallback.call("Successfully authenticated as ${token.username}!")
            serviceStatusLabel.text = 'API Connected'
            updateApiStatusIndicator('SUCCESS')
        } catch (Exception e) {
            appendOutputCallback.call("ERROR: ${e.message}")
            serviceStatusLabel.text = 'Failed'
            updateApiStatusIndicator('ERROR')
        }
    }

    private void updateApiStatusIndicator(String status) {
        apiStatusIndicator.styleClass.removeAll('status-success', 'status-error', 'status-unknown')

        switch (status) {
            case 'SUCCESS':
                apiStatusIndicator.styleClass.add('status-success')
                break
            case 'ERROR':
                apiStatusIndicator.styleClass.add('status-error')
                break
            default:
                apiStatusIndicator.styleClass.add('status-unknown')
        }
    }
}
