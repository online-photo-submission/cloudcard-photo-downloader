package com.cloudcard.photoDownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class Application {
    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static final String API_URL = "https://api.onlinephotosubmission.com/api";
    private static ConfigurableApplicationContext context;

    public static void main(String[] args) throws Exception {

//        TODO: Allow users to opt out of remote configs.
        context = new SpringApplicationBuilder(Application.class)
                .initializers(new RemoteConfigInitializer())
                .run(args);
    }

    public static void restart() {
        // 1. Grab the arguments from the current context before we kill it
        ApplicationArguments args = context.getBean(ApplicationArguments.class);
        String[] sourceArgs = args.getSourceArgs();

        // 2. Start a new non-daemon thread so the JVM stays alive even after the main context closes
        Thread restartThread = new Thread(() -> {
            try {
                log.info("Closing current Spring context...");
                context.close();

                // Give the OS a second to release file locks or ports
                Thread.sleep(2000);

                log.info("Starting new Spring context...");
                context = new SpringApplicationBuilder(Application.class)
                        .initializers(new RemoteConfigInitializer())
                        .run(sourceArgs);

            } catch (Exception e) {
                log.error("Failed to restart application!", e);
                System.exit(1);
            }
        });

        restartThread.setDaemon(false);
        restartThread.start();
    }
}
