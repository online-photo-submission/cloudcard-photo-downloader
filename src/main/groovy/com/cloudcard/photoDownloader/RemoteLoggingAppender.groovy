package com.cloudcard.photoDownloader

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.Level
import ch.qos.logback.classic.spi.IThrowableProxy
import ch.qos.logback.classic.spi.ThrowableProxyUtil
import ch.qos.logback.core.AppenderBase

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * This will allow error logs to be posted to the RemotePhoto API, either every 60 seconds or once the batch size reaches
 * 100 messages.
 *
 * DO NOT use any log methods here to prevent infinite looping (stick to System.out/err)
 */
class RemoteLoggingAppender extends AppenderBase<ILoggingEvent> {

    ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>()
    private final int BATCH_SIZE = 100;
    private final long MAX_DELAY_MS = 60000;
    private ScheduledExecutorService scheduler;

    @Override
    void start() {
        super.start();
        // Start a background timer to flush logs even if BATCH_SIZE isn't reached
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(this::flush, MAX_DELAY_MS, MAX_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        if (eventObject.level == Level.ERROR) {
            StringBuilder fullMessage = new StringBuilder(eventObject.getFormattedMessage());

            // Check if there is an associated exception (Throwable)
            IThrowableProxy throwableProxy = eventObject.getThrowableProxy();
            if (throwableProxy != null) {
                // Convert the proxy into a full stack trace string
                String stackTrace = ThrowableProxyUtil.asString(throwableProxy);
                fullMessage.append(System.lineSeparator()).append(stackTrace);
            }

            logQueue << fullMessage.toString()

            if (logQueue.size() >= BATCH_SIZE) {
                flush();
            }
        }
    }

    private synchronized void flush() {
        if (logQueue.isEmpty()) return;

        List<String> toSend = new ArrayList<>(logQueue);
        logQueue.clear();

        // Perform the network call to your remote service
        sendToRemoteService(toSend);
    }

    private void sendToRemoteService(List<String> events) {
        try {
            CloudCardClient client = SpringContextBridge.getBean(CloudCardClient.class)
            println("Sending batch of " + events.size() + " logs to remote service.");
            client.remoteLog(events)
        } catch (Exception ex) {
            System.err.println("Failed to load CloudCardClient from the context: ${ex.localizedMessage}")
            //clear the queue to prevent recurring problems if we get here
            logQueue.clear()
        }
    }

    @Override
    void stop() {
        flush(); // Final attempt to send remaining logs
        scheduler.shutdown();
        super.stop();
    }
}
