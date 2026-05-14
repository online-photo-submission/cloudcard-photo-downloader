package ai.remotephoto.setup

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.*
import javafx.scene.shape.Circle

import java.nio.file.Path

class SetupView {

    private static final Path APP_HOME = Path.of(System.getProperty('user.dir'))

    DownloaderConfigService downloaderConfigService = new DownloaderConfigService()

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

        Label title = new Label('RemotePhoto Downloader')
        title.style = '-fx-font-size: 22px; -fx-font-weight: bold;'

        Label subtitle = new Label('Configure the downloader & register it as a Windows service.')

        VBox titleContent = new VBox(4, title, subtitle)
        HBox titleRow = new HBox(18, logo, titleContent)
        titleRow.alignment = Pos.CENTER_LEFT
        HBox.setHgrow(titleContent, Priority.ALWAYS)

        updateApiStatusIndicator('UNKNOWN')
        HBox statusRow = new HBox(8, new Label('Service Status:'), apiStatusIndicator, serviceStatusLabel)
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

        logo.fitWidth = 400
        logo.fitHeight = 112
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

        GridPane.setHgrow(apiUrlField, Priority.ALWAYS)
        GridPane.setHgrow(patField, Priority.ALWAYS)
        GridPane.setHgrow(appHomeLabel, Priority.ALWAYS)
        return form
    }

    private VBox buildFooter() {
        Button testConnectionButton = new Button('Test Connection')
//        TODO: Consider moving this to a more sensible "save" location
        Button savePropertiesButton = new Button('Save Properties')
        Button installButton = new Button('Install Service')
        Button startButton = new Button('Start Service')
        Button stopButton = new Button('Stop Service')
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
                serviceStatusLabel.text = 'API Failed'
                updateApiStatusIndicator('ERROR')
            }
        }
        savePropertiesButton.onAction = {
            downloaderConfigService.writeOrUpdate(APP_HOME, apiUrlField.text, patField.text, integrationNameField.text, useRemoteConfigRadio.selected)

            appendOutput("Saved ${APP_HOME.resolve('application.properties')}")
        }
        startButton.onAction = { appendOutput('TODO: start CloudCardDownloader service') }
        stopButton.onAction = { appendOutput('TODO: stop CloudCardDownloader service') }
        refreshStatusButton.onAction = {
            serviceStatusLabel.text = 'TODO'
            appendOutput('TODO: refresh service status')
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
}
