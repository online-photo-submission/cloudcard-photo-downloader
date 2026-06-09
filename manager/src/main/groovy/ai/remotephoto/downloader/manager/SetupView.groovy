package ai.remotephoto.downloader.manager

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.shape.Circle

import java.nio.file.Path

class SetupView {

    private static final Path APP_HOME = resolveAppHome()

//    TODO: We have this referenced twice now lol
    private static final Set<String> MANAGED_PROPERTY_KEYS = [
        'cloudcard.api.url',
        'cloudcard.api.accessToken',
        'cloudcard.integrationName',
        'downloader.useRemoteConfigs'
    ] as Set

    DownloaderConfigService downloaderConfigService = new DownloaderConfigService()
//    ServyConfigWriter servyConfigWriter = new ServyConfigWriter()
//    ServyServiceManager servyServiceManager = new ServyServiceManager()

    Properties properties = downloaderConfigService.loadProperties(APP_HOME)
    final Boolean useRemoteConfigs = properties?.getProperty('downloader.useRemoteConfigs', 'true')?.toBoolean()

    final Label serviceStatusLabel = new Label('Unknown')
    final Circle apiStatusIndicator = new Circle(6)

    final TextField apiUrlField = new TextField(properties?.get('cloudcard.api.url') as String ?: 'https://api.cloudcard.us/api')
    final PasswordField patField = new PasswordField(text: properties?.get('cloudcard.api.accessToken') as String ?: '')
    final TextField integrationNameField = new TextField(properties?.get('cloudcard.integrationName') as String ?: 'Downloader')

    final ToggleGroup remoteConfigToggleGroup = new ToggleGroup()
    final RadioButton useRemoteConfigRadio = new RadioButton(selected: useRemoteConfigs, text: 'Use remote config')
    final RadioButton useLocalConfigRadio = new RadioButton(selected: !useRemoteConfigs, text: 'Use local config')

    final Label additionalPropertiesLabel = new Label('Advanced Overrides')
    final TextArea additionalPropertiesArea = new TextArea()
    final VBox additionalPropertiesBox = new VBox(4)

    final TextArea outputArea = new TextArea()

    BorderPane buildRoot() {
        BorderPane root = new BorderPane()
        root.style = '-fx-font-size: 16px;'
        root.padding = new Insets(18)

        root.top = buildHeader()
        root.center = buildForm()
        root.bottom = buildFooter()

        return root
    }

    private VBox buildHeader() {
        ImageView logo = buildLogo()

        HBox titleRow = new HBox(70, logo)
        titleRow.alignment = Pos.TOP_LEFT

        updateApiStatusIndicator('UNKNOWN')
        HBox statusRow = new HBox(8, new Label('API Connection:'), apiStatusIndicator, serviceStatusLabel)
        statusRow.alignment = Pos.CENTER_LEFT
        serviceStatusLabel.style = '-fx-font-weight: bold;'

        VBox header = new VBox(10, titleRow, statusRow)
        header.padding = new Insets(0, 0, 18, 0)
        return header
    }

    private ImageView buildLogo() {
        URL logoUrl = getClass().getResource('/logo.png')

        ImageView logo = logoUrl
            ? new ImageView(new Image(logoUrl.toExternalForm()))
            : new ImageView()

        logo.fitWidth = 350
        logo.fitHeight = 250
        logo.preserveRatio = true
        logo.smooth = true

        return logo
    }

    private GridPane buildForm() {
        GridPane form = new GridPane()
        form.hgap = 12
        form.vgap = 12
        form.padding = new Insets(0, 0, 18, 0)

        apiUrlField.promptText = 'CloudCard API URL'
        integrationNameField.promptText = 'Integration Name'
        patField.promptText = 'Paste token here'

        form.add(new Label('API URL'), 0, 0)
        form.add(apiUrlField, 1, 0)

        form.add(new Label('Integration Name'), 0, 1)
        form.add(integrationNameField, 1, 1)

        form.add(new Label('Persistent Access Token'), 0, 2)
        form.add(patField, 1, 2)

        Label appHomeLabel = new Label(APP_HOME.toString())
        appHomeLabel.style = '-fx-font-family: monospace;'

        form.add(new Label('Application Home'), 0, 3)
        form.add(appHomeLabel, 1, 3)

        useRemoteConfigRadio.toggleGroup = remoteConfigToggleGroup
        useLocalConfigRadio.toggleGroup = remoteConfigToggleGroup

        HBox remoteConfigRow = new HBox(12, useRemoteConfigRadio, useLocalConfigRadio)
        remoteConfigRow.alignment = Pos.CENTER_LEFT

        form.add(new Label('Configuration Mode'), 0, 4)
        form.add(remoteConfigRow, 1, 4)

        configureAdditionalPropertiesControls()
        loadAdditionalProperties()
        form.add(additionalPropertiesLabel, 0, 5)
        form.add(additionalPropertiesBox, 1, 5)
        GridPane.setValignment(additionalPropertiesLabel, javafx.geometry.VPos.TOP)

        GridPane.setHgrow(apiUrlField, Priority.ALWAYS)
        GridPane.setHgrow(patField, Priority.ALWAYS)
        GridPane.setHgrow(appHomeLabel, Priority.ALWAYS)
        GridPane.setHgrow(additionalPropertiesBox, Priority.ALWAYS)
        GridPane.setVgrow(additionalPropertiesBox, Priority.ALWAYS)
        return form
    }

    private void configureAdditionalPropertiesControls() {
        Label help = new Label('Optional application.properties overrides (note: RemoteConfigs will override these).')
        help.style = '-fx-font-size: 14px; -fx-text-fill: #6b7280;'

        Label examples = new Label('Example: downloader.fetchStatuses=APPROVED')
        examples.style = '-fx-font-family: monospace; -fx-font-size: 14px; -fx-text-fill: #6b7280;'

        additionalPropertiesArea.promptText = 'Optional advanced key=value overrides, one per line'
        additionalPropertiesArea.prefRowCount = 6
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

    private VBox buildFooter() {
        Button testConnectionButton = new Button('Test Connection')
//        TODO: Consider moving this to a more sensible "save" location
        Button savePropertiesButton = new Button('Save Properties')
        Button installButton = new Button('Install Service')
        Button startButton = new Button('Start Service')
        Button stopButton = new Button('Stop Service')
//        TODO: Actually display the status
        Button refreshStatusButton = new Button('Refresh Status')

        testConnectionButton.onAction = {
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
        savePropertiesButton.onAction = {
            downloaderConfigService.writeOrUpdate(
                APP_HOME,
                apiUrlField.text,
                patField.text,
                integrationNameField.text,
                useRemoteConfigRadio.selected,
                additionalPropertiesArea.text
            )

            appendOutput("Saved ${APP_HOME.resolve('application.properties')}")
//            Reload the properties to reflect the updated file
            loadAdditionalProperties()
        }
        installButton.onAction = {
//            TODO: Consider if we want to writeOrUpdate before installing.
//            downloaderConfigService.writeOrUpdate(...)

            Path json = ServyConfigWriter.write(APP_HOME)

            try {
                appendOutput(ServyServiceManager.install(APP_HOME, json))
                appendOutput('Installed CloudCardDownloader service.')
            } catch (Exception e) {
                appendOutput("Failed to install service: ${e.message}")
            }
        }
        startButton.onAction = {
            try {
                appendOutput(ServyServiceManager.start(APP_HOME))
            } catch (Exception e) {
                appendOutput("Failed to start service: ${e.message}")
            }
        }

        stopButton.onAction = {
            try {
                appendOutput(ServyServiceManager.stop(APP_HOME))
            } catch (Exception e) {
                appendOutput("Failed to stop service: ${e.message}")
            }
        }
        refreshStatusButton.onAction = {
            try {
                appendOutput(ServyServiceManager.refresh(APP_HOME))
            } catch (Exception e) {
                appendOutput("Failed to refresh service: ${e.message}")
            }
        }

        HBox buttons = new HBox(10, testConnectionButton, savePropertiesButton, installButton, startButton, stopButton, refreshStatusButton)
        buttons.alignment = Pos.CENTER_LEFT

        outputArea.editable = false
        outputArea.wrapText = true
        outputArea.promptText = 'Installer output will appear here.'
        outputArea.style = '-fx-font-size: 12px;'
        VBox.setVgrow(outputArea, Priority.ALWAYS)

        VBox footer = new VBox(12, buttons, outputArea)
        footer.padding = new Insets(12, 0, 0, 0)
        return footer
    }

    private void updateApiStatusIndicator(String status) {
        switch (status) {
            case 'SUCCESS':
                apiStatusIndicator.style = '-fx-fill: #22c55e;'
                break
            case 'ERROR':
                apiStatusIndicator.style = '-fx-fill: #ef4444;'
                break
            default:
                apiStatusIndicator.style = '-fx-fill: #9ca3af;'
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
