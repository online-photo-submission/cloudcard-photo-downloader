package ai.remotephoto.downloader.manager.ui
import java.awt.Desktop
import java.nio.file.Files
import java.nio.file.Path

class DesktopUtility {

    static Path resolveAppHome() {
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

    static String openHelpFile(Path appHome) {
        Path readmeFile = appHome.resolve('MANAGER.README.txt')

        if (!Files.exists(readmeFile)) {
            return "MANAGER.README.txt was not found: ${readmeFile}"
        }

        openFile(readmeFile)

        return "Opened help file: ${readmeFile}"
    }

    static String openLogFolder(Path appHome) {
        Path logsDir = appHome.resolve('logs')
        Files.createDirectories(logsDir)
        openFile(logsDir)

        return "Opened log folder: ${logsDir}"
    }

    private static void openFile(Path path) {
        if (!Desktop.isDesktopSupported()) {
            throw new IllegalStateException("Opening files is not supported on this system: ${path}")
        }
        Desktop.desktop.open(path.toFile())
    }


}
