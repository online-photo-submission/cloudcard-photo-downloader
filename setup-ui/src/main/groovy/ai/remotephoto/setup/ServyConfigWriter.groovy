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
            ExecutablePath           : resolveJavaExecutable(),
            StartupDirectory         : appHome.toString(),
            Parameters               : '-jar cloudcard-photo-downloader.jar',
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
