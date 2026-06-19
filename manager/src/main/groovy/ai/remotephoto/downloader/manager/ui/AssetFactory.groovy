package ai.remotephoto.downloader.manager.ui

import javafx.scene.image.Image
import javafx.scene.image.ImageView

class AssetFactory {

    static ImageView buildLogo() {
        URL logoUrl = AssetFactory.getResource('/logo.png')

        ImageView logo = logoUrl
            ? new ImageView(new Image(logoUrl.toExternalForm()))
            : new ImageView()

        logo.fitWidth = 320
        logo.fitHeight = 110
        logo.preserveRatio = true
        logo.smooth = true

        return logo
    }

    static ImageView icon(String name) {
        URL iconUrl = AssetFactory.getResource("/icons/${name}.png")

        ImageView icon = iconUrl
            ? new ImageView(new Image(iconUrl.toExternalForm()))
            : new ImageView()

        icon.fitWidth = 16
        icon.fitHeight = 16
        icon.preserveRatio = true
        icon.smooth = true

        return icon
    }
}
