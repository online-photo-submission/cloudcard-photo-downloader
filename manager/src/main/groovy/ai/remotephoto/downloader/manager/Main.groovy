package ai.remotephoto.downloader.manager

import ai.remotephoto.downloader.manager.ui.MainView
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class Main extends Application {

    @Override
    void start(Stage stage) {
        MainView mainView = new MainView()

        stage.title = 'RemotePhoto Downloader Manager'

        URL iconUrl = getClass().getResource('/icon.png')
        if (iconUrl) {
            stage.icons.add(new Image(iconUrl.toExternalForm()))
        } else {
            println 'icon.png not found on classpath'
        }

        stage.scene = new Scene(mainView.buildCoreLayout(), 1040, 980)
        stage.show()
    }

    static void main(String[] args) {
        launch(Main, args)
    }
}
