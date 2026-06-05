package ai.remotephoto.setup

import groovy.json.JsonOutput

import java.nio.file.Files
import java.nio.file.Path

class ServyConfigWriter {

    static final String SERVICE_NAME = 'CloudCardDownloader'

    static Path write(Path appHome) {
        Map config = [
            Name                     : SERVICE_NAME,
            DisplayName              : 'RemotePhoto Downloader',
            Description              : 'Downloads photos from RemotePhoto.',
            ExecutablePath           : resolveDownloaderExecutable(appHome),
            StartupDirectory         : appHome.toString(),
            Parameters               : resolveParameters(appHome),
            StartupType              : 2,
            Priority                 : 2,
            EnableConsoleUI          : false,
            StdoutPath               : appHome.resolve('downloader.out.log').toString(),
            StderrPath               : appHome.resolve('downloader.err.log').toString(),
            EnableSizeRotation       : false,
            EnableDateRotation       : false,
            EnableHealthMonitoring   : false,
            RecoveryAction           : 1,
            MaxRestartAttempts       : 3,
            StartTimeout             : 10,
            StopTimeout              : 5
        ]

        Path jsonFile = appHome.resolve('downloader-service.json')

        Files.writeString(
            jsonFile,
            JsonOutput.prettyPrint(JsonOutput.toJson(config))
        )

        return jsonFile
    }

    private static String resolveDownloaderExecutable(Path appHome) {
        if (hasPackagedDownloaderLauncher(appHome)) {
            return appHome.parent.resolve('CloudCard Downloader.exe').toString()
        }

        return resolveJavaExecutable()
    }

    private static String resolveParameters(Path appHome) {
        if (hasPackagedDownloaderLauncher(appHome)) {
            return ''
        }

        return '-jar cloudcard-photo-downloader.jar'
    }

    private static boolean hasPackagedDownloaderLauncher(Path appHome) {
        if (!isWindows()) {
            return false
        }

        Path launcher = appHome.parent?.resolve('CloudCard Downloader.exe')

        return launcher && Files.exists(launcher)
    }

    private static String resolveJavaExecutable() {
        Path javaHome = Path.of(System.getProperty('java.home'))

        return javaHome
            .resolve('bin')
            .resolve(isWindows() ? 'java.exe' : 'java')
            .toString()
    }

    private static boolean isWindows() {
        System.getProperty('os.name')
            .toLowerCase()
            .contains('win')
    }
}
