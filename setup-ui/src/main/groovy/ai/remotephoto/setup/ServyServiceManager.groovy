package ai.remotephoto.setup

import java.nio.file.Path

class ServyServiceManager {

//    Overwriting the install (as long as the name matches) is fine to just spam
    static String install(Path appHome, Path configFile) {
        List<String> command = [
            servyExecutable(appHome),
            'import',
            '--config', 'json',
            '--path', configFile.toString(),
            '--install'
        ]

        if (!isWindows()) return command.join(' ')

        run(appHome, command)

        return 'Installed service.'
    }

    static String start(Path appHome) {
        List<String> command = [
            servyExecutable(appHome),
            'start',
            '--name', ServyConfigWriter.SERVICE_NAME
        ]

        if(!isWindows()) return command.join(' ')

        run(appHome, command)

        return 'Started service.'
    }

    static String stop(Path appHome) {
        List<String> command = [
            servyExecutable(appHome),
            'stop',
            '--name', ServyConfigWriter.SERVICE_NAME
        ]

        if(!isWindows()) return command.join(' ')

        run(appHome, command)

        return 'Stopped service.'
    }

//    TODO: Capture the refreshed status or consider polling this every 5 seconds while the UI is open!
    static String refresh(Path appHome) {
        List<String> command = [
            servyExecutable(appHome),
            'status',
            '--name', ServyConfigWriter.SERVICE_NAME
        ]

        if(!isWindows()) return command.join(' ')

        run(appHome, command)

//        TODO: This is dumb! It doesn't say what the actual status is lol
        return 'Refreshed service.'
    }

    private static void run(Path workingDir, List<String> command) {
        Process process = new ProcessBuilder(command)
            .directory(workingDir.toFile())
            .redirectErrorStream(true)
            .start()

        String output = process.inputStream.text
        int exitCode = process.waitFor()

        if (exitCode != 0) {
            throw new RuntimeException(output)
        }
    }

//    TODO: Do we need this? It is odd.
    private static String servyExecutable(Path appHome) {
        return appHome.resolve('servy').resolve('servy-cli.exe')
                      .toString()
    }

    private static boolean isWindows() {
        System.getProperty('os.name')
            .toLowerCase()
            .contains('win')
    }
}
