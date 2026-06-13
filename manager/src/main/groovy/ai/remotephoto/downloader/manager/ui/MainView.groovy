package ai.remotephoto.downloader.manager.ui

import ai.remotephoto.downloader.manager.api.ApiUtil
import ai.remotephoto.downloader.manager.api.AuthenticationToken
import ai.remotephoto.downloader.manager.service.DownloaderConfigService
import ai.remotephoto.downloader.manager.service.ServyConfigWriter
import ai.remotephoto.downloader.manager.service.ServyServiceManager
import javafx.concurrent.Task
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.shape.Circle

import java.nio.file.Path

class MainView {

    DownloaderConfigService downloaderConfigService = new DownloaderConfigService()
    MainViewModel viewModel = new MainViewModel()

    private static final Path APP_HOME = DesktopUtility.resolveAppHome()

    // Status Indicators
    final Label serviceStatusLabel = new Label('Unknown')
    final Circle apiStatusIndicator = new Circle(6)

    final TextField apiUrlField = new TextField()
    final PasswordField patField = new PasswordField()
    final TextField visiblePatField = new TextField()
    final Button revealTokenButton = new Button()
    final TextField integrationNameField = new TextField()

    final ToggleGroup remoteConfigToggleGroup = new ToggleGroup()
    final RadioButton useRemoteConfigRadio = new RadioButton('Remote [Recommended]')
    final RadioButton useLocalConfigRadio = new RadioButton('Local [Requires Advanced Settings]')

    final TextArea advancedPropertiesArea = new TextArea()
    final VBox additionalPropertiesBox = new VBox(4)
    final ProgressIndicator busyIndicator = new ProgressIndicator()
    final Label busyLabel = new Label('')

    final TextArea outputArea = new TextArea()

    MainView() {
        // Build the actual visuals (text boxes, grids, cards)
        buildCoreLayout()

        // Bind the visual text boxes to the ViewModel properties
        bindFormFields()

        bindStatusIndicators()

        // Have the model to load the file data.
        viewModel.loadConfiguration(APP_HOME, downloaderConfigService)
    }

    BorderPane buildCoreLayout() {
        viewModel.logConsumer = { String message ->
            outputArea.appendText("${new Date()}  ${message}${System.lineSeparator()}")
        }
        
        BorderPane root = new BorderPane()
        root.styleClass.add('root-pane')


        URL stylesheet = getClass().getResource('/manager.css')

        if (stylesheet) {
            root.stylesheets.add(stylesheet.toExternalForm())
        }

        root.padding = new Insets(18)

        VBox content = new VBox(14, buildConnectionCard(), buildApplicationCard(), buildAdvancedOverridesCard(), buildServiceCard(), buildActivityLogCard())
        VBox.setVgrow(content, Priority.ALWAYS)

        root.top = buildHeader()

        ScrollPane scrollPane = new ScrollPane(content)
        scrollPane.fitToWidth = true
        scrollPane.hbarPolicy = ScrollPane.ScrollBarPolicy.NEVER
        scrollPane.vbarPolicy = ScrollPane.ScrollBarPolicy.ALWAYS
        scrollPane.styleClass.add('main-scroll-pane')

        root.center = scrollPane
        return root
    }

    private VBox buildHeader() {
        ImageView logo = AssetFactory.buildLogo()

        Button helpButton = new Button('Help')
        helpButton.styleClass.add('outline-button')
        helpButton.onAction = { DesktopUtility.openHelpFile(APP_HOME) }
        Region headerSpacer = new Region()
        HBox.setHgrow(headerSpacer, Priority.ALWAYS)
        HBox titleRow = new HBox(12, logo, headerSpacer, helpButton)
        titleRow.alignment = Pos.TOP_LEFT

        updateApiStatusIndicator('UNKNOWN')
        HBox statusRow = new HBox(8, new Label('API Connection:'), apiStatusIndicator, serviceStatusLabel)
        statusRow.styleClass.add('status-row')
        statusRow.alignment = Pos.CENTER_LEFT
        serviceStatusLabel.styleClass.add('status-label')

        VBox header = new VBox(10, titleRow, statusRow)
        header.padding = new Insets(0, 0, 18, 0)
        return header
    }

    private VBox buildConnectionCard() {
        GridPane form = new GridPane()
        form.hgap = 12
        form.vgap = 12

        apiUrlField.promptText = 'CloudCard API URL'
        integrationNameField.promptText = 'Integration Name'
        patField.promptText = 'Paste token here'
        visiblePatField.promptText = 'Paste token here'
        visiblePatField.visible = false
        visiblePatField.managed = false
        visiblePatField.textProperty().bindBidirectional(patField.textProperty())

        revealTokenButton.focusTraversable = false
        revealTokenButton.styleClass.add('icon-button')
        revealTokenButton.graphic = AssetFactory.icon('eye')
        revealTokenButton.onAction = {
            boolean currentlyVisible = visiblePatField.visible

            visiblePatField.visible = !currentlyVisible
            visiblePatField.managed = !currentlyVisible
            patField.visible = currentlyVisible
            patField.managed = currentlyVisible
            revealTokenButton.graphic = AssetFactory.icon(currentlyVisible ? 'eye' : 'eye-off')
        }

        Button testConnectionButton = new Button('Test Connection')
        testConnectionButton.graphic = AssetFactory.icon('link')
        testConnectionButton.styleClass.add('secondary-button')
        testConnectionButton.onAction = {
            testConnection()
        }

        form.add(new Label('API URL'), 0, 0)
        form.add(apiUrlField, 1, 0)

        form.add(new Label('Integration Name'), 0, 1)
        form.add(integrationNameField, 1, 1)

        StackPane tokenStack = new StackPane(patField, visiblePatField)
        HBox.setHgrow(tokenStack, Priority.ALWAYS)

        HBox tokenRow = new HBox(10, tokenStack, revealTokenButton, testConnectionButton)
        tokenRow.alignment = Pos.CENTER_LEFT

        form.add(new Label('Persistent Access Token'), 0, 2)
        form.add(tokenRow, 1, 2)

        GridPane.setHgrow(apiUrlField, Priority.ALWAYS)
        GridPane.setHgrow(patField, Priority.ALWAYS)
        GridPane.setHgrow(tokenRow, Priority.ALWAYS)

        return card('Connection', form)
    }

    private VBox buildApplicationCard() {
        GridPane form = new GridPane()
        form.hgap = 12
        form.vgap = 12

        Label appHomeLabel = new Label(APP_HOME.toString())
        appHomeLabel.styleClass.add('monospace')

        useRemoteConfigRadio.toggleGroup = remoteConfigToggleGroup
        useLocalConfigRadio.toggleGroup = remoteConfigToggleGroup

        HBox remoteConfigRow = new HBox(12, useRemoteConfigRadio, useLocalConfigRadio)
        remoteConfigRow.alignment = Pos.CENTER_LEFT

        form.add(new Label('Application Home'), 0, 0)
        form.add(appHomeLabel, 1, 0)
        form.add(new Label('Configuration Mode'), 0, 1)
        form.add(remoteConfigRow, 1, 1)

        GridPane.setHgrow(appHomeLabel, Priority.ALWAYS)

        return card('Application', form)
    }

    private TitledPane buildAdvancedOverridesCard() {
        configureAdditionalPropertiesControls()

        TitledPane advancedPane = new TitledPane('Advanced Settings', additionalPropertiesBox)
        advancedPane.expanded = false
        advancedPane.collapsible = true
        advancedPane.animated = false
        advancedPane.styleClass.add('advanced-pane')

        return advancedPane
    }

    private VBox card(String title, Region content) {
        return card(title, content, null)
    }

    private VBox card(String title, Region content, Node titleAccessory) {
        return card(title, content, titleAccessory, true)
    }

    private VBox card(String title, Region content, Node titleAccessory, boolean alignAccessoryRight) {
        Label titleLabel = new Label(title)
        titleLabel.styleClass.add('section-title')
        Region spacer = new Region()
        HBox.setHgrow(spacer, Priority.ALWAYS)

        HBox titleRow

        if (titleAccessory && alignAccessoryRight) {
            titleRow = new HBox(10, titleLabel, spacer, titleAccessory)
        } else if (titleAccessory) {
            titleRow = new HBox(10, titleLabel, titleAccessory, spacer)
        } else {
            titleRow = new HBox(10, titleLabel, spacer)
        }

        titleRow.alignment = Pos.CENTER_LEFT
        VBox card = new VBox(10, titleRow, content)
        card.styleClass.add('card')

        return card
    }

    private void configureAdditionalPropertiesControls() {
        Label help = new Label('Optional application.properties overrides (note: Remote Configurations will override these).')
        help.styleClass.add('muted')

        Label examples = new Label('Example: downloader.fetchStatuses=APPROVED')
        examples.styleClass.addAll('muted', 'monospace')

        advancedPropertiesArea.promptText = 'Optional advanced key=value overrides, one per line'
        advancedPropertiesArea.prefRowCount = 10
        advancedPropertiesArea.minHeight = 40
        advancedPropertiesArea.wrapText = false

        additionalPropertiesBox.children.setAll(help, examples, advancedPropertiesArea)
        VBox.setVgrow(advancedPropertiesArea, Priority.ALWAYS)
    }

    private VBox buildServiceCard() {
        Button applyConfigurationButton = new Button('Apply Configuration')
        Button startButton = new Button('Start')
        Button stopButton = new Button('Stop')
        Button refreshStatusButton = new Button('Refresh')

        applyConfigurationButton.graphic = AssetFactory.icon('save')
        startButton.graphic = AssetFactory.icon('play')
        stopButton.graphic = AssetFactory.icon('stop-circle')
        refreshStatusButton.graphic = AssetFactory.icon('refresh-cw')

        applyConfigurationButton.styleClass.add('primary-button')
        startButton.styleClass.add('success-button')
        stopButton.styleClass.add('danger-button')
        refreshStatusButton.styleClass.add('secondary-button')

        busyIndicator.prefWidth = 18
        busyIndicator.prefHeight = 18
        busyIndicator.progress = ProgressIndicator.INDETERMINATE_PROGRESS
        busyLabel.styleClass.add('muted')

        applyConfigurationButton.onAction = {
            runBackground('Applying configuration') {
                saveConfiguration()
                Path json = ServyConfigWriter.write(APP_HOME)
                String installOutput = ServyServiceManager.install(APP_HOME, json)
                return "${installOutput}\nApplied configuration and installed CloudCardDownloader service."
            }
        }

        startButton.onAction = {
            runBackground('Starting service') {
                ServyServiceManager.start(APP_HOME)
            }
        }

        stopButton.onAction = {
            runBackground('Stopping service') {
                ServyServiceManager.stop(APP_HOME)
            }
        }

        refreshStatusButton.onAction = {
            runBackground('Refreshing service status') {
                ServyServiceManager.refresh(APP_HOME)
            }
        }

        HBox busyRow = new HBox(8, busyIndicator, busyLabel)
        busyRow.alignment = Pos.CENTER_LEFT

        HBox buttons = new HBox(10, applyConfigurationButton, startButton, stopButton, refreshStatusButton)
        buttons.alignment = Pos.CENTER_LEFT

        return card('Downloader Service', buttons, busyRow, false)
    }

    private VBox buildActivityLogCard() {
        Button openLogsButton = new Button('Open Logs Folder')
        openLogsButton.graphic = AssetFactory.icon('code')
        openLogsButton.styleClass.add('outline-button')
        openLogsButton.onAction = { DesktopUtility.openLogFolder(APP_HOME) }

        outputArea.editable = false
        outputArea.wrapText = true
        outputArea.minHeight = 180
        outputArea.prefHeight = 220

        outputArea.styleClass.addAll('activity-log', 'monospace')
        VBox.setVgrow(outputArea, Priority.ALWAYS)

        return card('Console Output', outputArea, openLogsButton)
    }

    private void runBackground(String actionName, Closure<String> work) {
        // Declaring the state updates the UI automatically through bindings
        viewModel.isProcessing.set(true)
        viewModel.processingMessage.set("${actionName}...")

        Task<String> task = new Task<String>() {
            @Override protected String call() { return work.call() }
        }

        task.setOnSucceeded {
            viewModel.isProcessing.set(false) // Automatically hides indicator
            if (task.value?.trim()) viewModel.log(task.value.trim())
        }

        task.setOnFailed {
            viewModel.isProcessing.set(false) // Automatically hides indicator
            viewModel.log("${actionName} failed: ${task.exception?.message ?: 'Unknown error'}")
        }

        Thread worker = new Thread(task)
        worker.daemon = true
        worker.start()
    }

    private void testConnection() {
        ApiUtil apiClient = new ApiUtil()

        try {
            AuthenticationToken token = apiClient.authenticate(patField.text, apiUrlField.text)
            viewModel.log("Successfully authenticated as ${token.username}!")
            serviceStatusLabel.text = 'API Connected'
            updateApiStatusIndicator('SUCCESS')
        } catch (Exception e) {
            viewModel.log("ERROR: ${e.message}")
            serviceStatusLabel.text = 'Failed'
            updateApiStatusIndicator('ERROR')
        }
    }

    private void saveConfiguration() {
        downloaderConfigService.writeOrUpdate(
            APP_HOME,
            apiUrlField.text,
            patField.text,
            integrationNameField.text,
            useRemoteConfigRadio.selected,
            advancedPropertiesArea.text
        )

        viewModel.log("Saved ${APP_HOME.resolve('application.properties')}")

        // Reload the properties so the user can see invalid ones were automatically removed.
        viewModel.loadConfiguration(APP_HOME, downloaderConfigService)
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

    private void bindFormFields() {
        apiUrlField.textProperty().bindBidirectional(viewModel.apiUrl)
        integrationNameField.textProperty().bindBidirectional(viewModel.integrationName)
        patField.textProperty().bindBidirectional(viewModel.accessToken)
        advancedPropertiesArea.textProperty().bindBidirectional(viewModel.advancedProperties)

        useRemoteConfigRadio.selectedProperty().bindBidirectional(viewModel.useRemoteConfigs)

        // Local configs radio button must always be the exact opposite of the remote button
        useLocalConfigRadio.selectedProperty().bind(useRemoteConfigRadio.selectedProperty().not())
    }

    private void bindStatusIndicators() {
        busyIndicator.visibleProperty().bind(viewModel.isProcessing)
        busyIndicator.managedProperty().bind(viewModel.isProcessing)

        busyLabel.textProperty().bind(viewModel.processingMessage)
        busyLabel.visibleProperty().bind(viewModel.isProcessing)
        busyLabel.managedProperty().bind(viewModel.isProcessing)
    }
}
