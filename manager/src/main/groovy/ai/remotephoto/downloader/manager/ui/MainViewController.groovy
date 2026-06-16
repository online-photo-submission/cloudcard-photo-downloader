package ai.remotephoto.downloader.manager.ui

import ai.remotephoto.downloader.manager.desktop.DesktopUtility
import javafx.beans.value.ChangeListener
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.shape.Circle
import java.nio.file.Path

class MainViewController {

    // --- FXML Injected UI Elements ---
    @FXML private BorderPane root
    @FXML private HBox logoContainer
    @FXML private Label serviceStatusLabel
    @FXML private Circle apiStatusIndicator
    @FXML private TextField apiUrlField
    @FXML private TextField integrationNameField
    @FXML private PasswordField patField
    @FXML private TextField visiblePatField
    @FXML private Button revealTokenButton
    @FXML private Button testConnectionButton
    @FXML private Label appHomeLabel
    @FXML private RadioButton useRemoteConfigRadio
    @FXML private RadioButton useLocalConfigRadio
    @FXML private ToggleGroup remoteConfigToggleGroup
    @FXML private TextArea advancedPropertiesArea
    @FXML private VBox additionalPropertiesBox
    @FXML private ProgressIndicator busyIndicator
    @FXML private Label busyLabel
    @FXML private Button saveConfigurationButton
    @FXML private Button startButton
    @FXML private Button stopButton
    @FXML private Button refreshStatusButton
    @FXML private Button openLogsButton
    @FXML private TextArea outputArea

    // --- Backend States & Utilities ---
    private final MainViewModel viewModel = new MainViewModel()
    private static final Path APP_HOME = DesktopUtility.resolveAppHome()

    /**
     * JavaFX Lifecycle Method. Automatically called after the FXML file has been loaded
     * and all @FXML fields have been injected.
     */
    @FXML
    void initialize() {
        logoContainer.children.add(AssetFactory.buildLogo())

        setupDynamicAssets()
        bindFormFields()
        bindStatusIndicators()

        // Link the ViewModel's logging pipeline straight to our FXML TextArea
        viewModel.logConsumer = { String message ->
            outputArea.appendText("${new Date()}  ${message}${System.lineSeparator()}")
        }

        // Initialize App Home display
        appHomeLabel.text = APP_HOME.toString()

        // Load persisted config state into ViewModel, then sync radio group UI
        viewModel.loadConfiguration(APP_HOME)
        syncRadioButtons()
    }

    // --- Action Handlers Routed from FXML ---

    @FXML
    void handleHelp() {
        DesktopUtility.openHelpFile(APP_HOME)
    }

    @FXML
    void handleToggleTokenVisibility() {
        boolean currentlyVisible = visiblePatField.visible

        visiblePatField.visible = !currentlyVisible
        visiblePatField.managed = !currentlyVisible
        patField.visible = currentlyVisible
        patField.managed = currentlyVisible

        revealTokenButton.graphic = AssetFactory.icon(currentlyVisible ? 'eye' : 'eye-off')
    }

    @FXML
    void handleTestConnection() {
        viewModel.testConnection()
    }

    @FXML
    void handleSaveConfiguration() {
        viewModel.saveConfiguration(APP_HOME)
    }

    @FXML
    void handleStartService() {
        viewModel.startService(APP_HOME)
    }

    @FXML
    void handleStopService() {
        viewModel.stopService(APP_HOME)
    }

    @FXML
    void handleRefreshStatus() {
        viewModel.refreshServiceStatus(APP_HOME)
    }

    @FXML
    void handleOpenLogs() {
        DesktopUtility.openLogFolder(APP_HOME)
    }

    // --- Private Setup & Data Binding Subroutines ---

    /**
     * Loads assets and icon graphics that cannot easily or dynamically be declared in raw FXML.
     */
    private void setupDynamicAssets() {
        revealTokenButton.graphic = AssetFactory.icon('eye')
        testConnectionButton.graphic = AssetFactory.icon('link')
        saveConfigurationButton.graphic = AssetFactory.icon('save')
        startButton.graphic = AssetFactory.icon('play')
        stopButton.graphic = AssetFactory.icon('stop-circle')
        refreshStatusButton.graphic = AssetFactory.icon('refresh-cw')
        openLogsButton.graphic = AssetFactory.icon('code')

        busyIndicator.progress = ProgressIndicator.INDETERMINATE_PROGRESS
    }

    private void bindFormFields() {
        // Link inputs to data model bidirectional
        apiUrlField.textProperty().bindBidirectional(viewModel.apiUrl)
        integrationNameField.textProperty().bindBidirectional(viewModel.integrationName)
        patField.textProperty().bindBidirectional(viewModel.accessToken)
        visiblePatField.textProperty().bindBidirectional(patField.textProperty())
        advancedPropertiesArea.textProperty().bindBidirectional(viewModel.advancedProperties)
    }

    private void bindStatusIndicators() {
        // Unidirectional text status mapping
        serviceStatusLabel.textProperty().bind(viewModel.apiStatusText)

        // API Indicator Color updates based on ViewModel state strings
        viewModel.apiStatusState.addListener({ observable, oldValue, newValue ->
            updateApiStatusIndicator(newValue)
        } as ChangeListener<String>)
        updateApiStatusIndicator(viewModel.apiStatusState.get())

        // Background worker busy-state indicator properties
        busyIndicator.visibleProperty().bind(viewModel.isProcessing)
        busyIndicator.managedProperty().bind(viewModel.isProcessing)
        busyLabel.textProperty().bind(viewModel.processingMessage)
        busyLabel.visibleProperty().bind(viewModel.isProcessing)
        busyLabel.managedProperty().bind(viewModel.isProcessing)
    }

    private void syncRadioButtons() {
        if (viewModel.useRemoteConfigs.get()) {
            remoteConfigToggleGroup.selectToggle(useRemoteConfigRadio)
        } else {
            remoteConfigToggleGroup.selectToggle(useLocalConfigRadio)
        }

        remoteConfigToggleGroup.selectedToggleProperty().addListener({ obs, oldToggle, newToggle ->
            if (newToggle != null) {
                viewModel.useRemoteConfigs.set(newToggle == useRemoteConfigRadio)
            }
        } as ChangeListener)
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