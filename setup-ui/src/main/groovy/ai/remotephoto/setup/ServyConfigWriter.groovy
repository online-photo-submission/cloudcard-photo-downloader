package ai.remotephoto.setup

import groovy.json.JsonOutput
import java.nio.file.Files
import java.nio.file.Path

class ServyConfigWriter {

    static final String SERVICE_NAME = 'CloudCardDownloader'

    static Path write(Path appHome) {
        // Define paths relative to the internal app folder layout
        Path downloaderDir = appHome.resolve('downloader')
        Path jarPath = downloaderDir.resolve('cloudcard-photo-downloader.jar')
        Path configPath = downloaderDir.resolve('application.properties')

        Map config = [
            Name                     : SERVICE_NAME,
            DisplayName              : 'RemotePhoto Downloader',
            Description              : 'Downloads photos from RemotePhoto.',
            ExecutablePath           : resolveJavaExecutable(),
            // CRITICAL: Set the execution working directory to the internal downloader/ subfolder
            // This allows Spring Boot to natively resolve relative folder hooks (like logs/)
            StartupDirectory         : appHome.toString(),
            // Pass absolute file reference mappings to fully isolate execution parameters
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
