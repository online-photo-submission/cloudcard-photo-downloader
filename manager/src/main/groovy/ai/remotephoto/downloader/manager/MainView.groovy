package ai.remotephoto.downloader.manager

import ai.remotephoto.downloader.manager.api.ApiUtil
import ai.remotephoto.downloader.manager.api.AuthenticationToken
import ai.remotephoto.downloader.manager.config.DownloaderConfigService
import ai.remotephoto.downloader.manager.service.ServyConfigWriter
import ai.remotephoto.downloader.manager.service.ServyServiceManager
import ai.remotephoto.downloader.manager.ui.AssetFactory
import ai.remotephoto.downloader.manager.ui.DesktopUtility
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

    private static final Path APP_HOME = resolveAppHome()

//    TODO: We have this referenced twice now lol. It's a weaker part of it.
    private static final Set<String> MANAGED_PROPERTY_KEYS = [
        'cloudcard.api.url',
        'cloudcard.api.accessToken',
        'cloudcard.integration.name',
        'downloader.useRemoteConfigs'
    ] as Set

    DownloaderConfigService downloaderConfigService = new DownloaderConfigService()

    Properties properties = downloaderConfigService.loadProperties(APP_HOME)
    final Boolean useRemoteConfigs = properties?.getProperty('downloader.useRemoteConfigs', 'true')?.toBoolean()

    final Label serviceStatusLabel = new Label('Unknown')
    final Circle apiStatusIndicator = new Circle(6)

    final TextField apiUrlField = new TextField(properties?.get('cloudcard.api.url') as String ?: 'https://api.cloudcard.us/api')
    final PasswordField patField = new PasswordField(text: properties?.get('cloudcard.api.accessToken') as String ?: '')
    final TextField visiblePatField = new TextField(text: properties?.get('cloudcard.api.accessToken') as String ?: '')
    final Button revealTokenButton = new Button()
    final TextField integrationNameField = new TextField(properties?.get('cloudcard.integration.name') as String ?: 'Downloader')

    final ToggleGroup remoteConfigToggleGroup = new ToggleGroup()
    final RadioButton useRemoteConfigRadio = new RadioButton(selected: useRemoteConfigs, text: 'Remote [Recommended]')
    final RadioButton useLocalConfigRadio = new RadioButton(selected: !useRemoteConfigs, text: 'Local [Requires Advanced Settings]')

    final Label additionalPropertiesLabel = new Label('Advanced Overrides')
    final TextArea additionalPropertiesArea = new TextArea()
    final VBox additionalPropertiesBox = new VBox(4)

    final TextArea outputArea = new TextArea()
    final ProgressIndicator busyIndicator = new ProgressIndicator()
    final Label busyLabel = new Label('')

    BorderPane buildRoot() {
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

//    private ImageView buildLogo() {
//        URL logoUrl = getClass().getResource('/logo.png')
//
//        ImageView logo = logoUrl
//            ? new ImageView(new Image(logoUrl.toExternalForm()))
//            : new ImageView()
//
//        logo.fitWidth = 320
//        logo.fitHeight = 110
//        logo.preserveRatio = true
//        logo.smooth = true
//
//        return logo
//    }
//
//    private ImageView AssetFactory.icon(String name) {
//        URL iconUrl = getClass().getResource("/${name}.png")
//
//        ImageView icon = iconUrl
//            ? new ImageView(new Image(iconUrl.toExternalForm()))
//            : new ImageView()
//
//        icon.fitWidth = 16
//        icon.fitHeight = 16
//        icon.preserveRatio = true
//        icon.smooth = true
//
//        return icon
//    }

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
        loadAdditionalProperties()

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

        additionalPropertiesArea.promptText = 'Optional advanced key=value overrides, one per line'
        additionalPropertiesArea.prefRowCount = 10
        additionalPropertiesArea.minHeight = 40
        additionalPropertiesArea.wrapText = false

        additionalPropertiesBox.children.setAll(help, examples, additionalPropertiesArea)
        VBox.setVgrow(additionalPropertiesArea, Priority.ALWAYS)
    }

    private void loadAdditionalProperties() {
        additionalPropertiesArea.text = properties
            .findAll { key, value -> !MANAGED_PROPERTY_KEYS.contains(key.toString()) }
            .collect { key, value -> "${key}=${value}" }
            .sort()
            .join(System.lineSeparator())
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

        busyIndicator.visible = false
        busyIndicator.managed = false
        busyIndicator.prefWidth = 18
        busyIndicator.prefHeight = 18
        busyIndicator.progress = ProgressIndicator.INDETERMINATE_PROGRESS
        busyLabel.styleClass.add('muted')
        busyLabel.visible = false
        busyLabel.managed = false

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
                appendOutput(task.value.trim())
            }
        }

        task.setOnFailed {
            busyIndicator.visible = false
            busyIndicator.managed = false
            busyLabel.visible = false
            busyLabel.managed = false

            Throwable exception = task.exception
            appendOutput("${actionName} failed: ${exception?.message ?: 'Unknown error'}")
        }

        Thread worker = new Thread(task)
        worker.daemon = true
        worker.start()
    }
    private void testConnection() {
        ApiUtil apiClient = new ApiUtil()

        try {
            AuthenticationToken token = apiClient.authenticate(patField.text, apiUrlField.text)
            appendOutput("Successfully authenticated as ${token.username}!")
            serviceStatusLabel.text = 'API Connected'
            updateApiStatusIndicator('SUCCESS')
        } catch (Exception e) {
            appendOutput("ERROR: ${e.message}")
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
            additionalPropertiesArea.text
        )

        appendOutput("Saved ${APP_HOME.resolve('application.properties')}")
        loadAdditionalProperties()
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

    void appendOutput(String message) {
        outputArea.appendText("${new Date()}  ${message}${System.lineSeparator()}")
    }

    private static boolean isWindows() {
        System.getProperty('os.name').toLowerCase().contains('win')
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
