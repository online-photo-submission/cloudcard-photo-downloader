package com.cloudcard.photoDownloader


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

import java.time.Duration
import java.time.Instant

class RemoteConfigService {

    private static final Logger log = LoggerFactory.getLogger(RemoteConfigService.class)

    private static final Duration CHECK_INTERVAL = Duration.ofMinutes(5)
    private Instant nextCheck = Instant.MIN

    String pat
    String apiUrl
    String integrationName
    Integer currentVersion

    @Autowired
    TokenService tokenService

    Integration fetchRemoteConfig() {
        // If we have a tokenService available, we prefer to use it so it can manage the token refreshes and limit unnecessary logins/logouts.
        if (this.tokenService) {
            log.debug("Using managed TokenService for remote configs")
            return RemotePhotoUtil.getRemoteConfig(apiUrl, integrationName, tokenService.authTokenValue)
        }

        // If no token service is available, fall back to manual login/logout for each config fetch.
        String authToken = RemotePhotoUtil.login(apiUrl, pat).tokenValue

        try {
            return RemotePhotoUtil.getRemoteConfig(apiUrl, integrationName, authToken)
        } finally {
            RemotePhotoUtil.logout(apiUrl, authToken)
        }
    }

    synchronized boolean shouldRefresh() {
        Instant now = Instant.now()

        if (now.isBefore(nextCheck)) return false

        nextCheck = now + CHECK_INTERVAL

        try {
            return isVersionDifferent()
        } catch (Exception e) {
            log.warn("Unable to check for remote configuration updates. Continuing with the current configuration.", e)
            return false
        }
    }

    private boolean isVersionDifferent() {
        Integration remoteIntegration = fetchRemoteConfig()

        if (remoteIntegration.version != currentVersion) {
            log.info("Remote config change detected! Old version: ${currentVersion}, New version: ${remoteIntegration.version}")
            this.currentVersion = remoteIntegration.version
            return true
        }

        return false
    }
}
