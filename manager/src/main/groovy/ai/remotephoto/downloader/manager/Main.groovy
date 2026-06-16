package ai.remotephoto.downloader.manager

import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class Main extends Application {

    @Override
    void start(Stage stage) {
        stage.title = 'RemotePhoto Downloader Manager'

        URL fxmlUrl = getClass().getResource('/MainView.fxml')

        if (!fxmlUrl) {
            throw new IllegalStateException("Could not find MainView.fxml on the classpath. Check your resource folder structure.")
        }

        FXMLLoader loader = new FXMLLoader(fxmlUrl)
        Parent rootNode = loader.load()

        URL iconUrl = getClass().getResource('/icons/icon.png')
        if (iconUrl) {
            stage.icons.add(new Image(iconUrl.toExternalForm()))
        } else {
            println 'icon.png not found on classpath'
        }

        stage.scene = new Scene(rootNode, 1040, 980)
        stage.show()
    }

    static void main(String[] args) {
        launch(Main, args)
    }
}