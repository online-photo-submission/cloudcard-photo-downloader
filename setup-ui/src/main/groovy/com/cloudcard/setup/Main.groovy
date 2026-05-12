package com.cloudcard.setup

import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Stage

class Main extends Application {

    @Override
    void start(Stage stage) {
        SetupView setupView = new SetupView()

        stage.title = 'CloudCard Setup'

        URL iconUrl = getClass().getResource('/icon.png')
        if (iconUrl) {
            stage.icons.add(new Image(iconUrl.toExternalForm()))
        } else {
            println 'icon.png not found on classpath'
        }

        stage.scene = new Scene(setupView.buildRoot(), 940, 780)
        stage.show()
    }

    static void main(String[] args) {
        launch(Main, args)
    }
}
