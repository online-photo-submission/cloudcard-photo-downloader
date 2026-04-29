package com.cloudcard.photoDownloader


import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class RemoteConfigService {
    private static final Logger log = LoggerFactory.getLogger(RemoteConfigService.class)

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
            return RemotePhotoUtil.getRemoteConfig(apiUrl, integrationName, tokenService.getAuthToken())
        }

        // If no token service is available, fall back to manual login/logout for each config fetch.
        String authToken = RemotePhotoUtil.login(apiUrl, pat)

        try {
            return RemotePhotoUtil.getRemoteConfig(apiUrl, integrationName, authToken)
        } finally {
                RemotePhotoUtil.logout(apiUrl, authToken)
        }
    }

    Boolean isUpdated() {
        Integration remoteIntegration = fetchRemoteConfig()

        if (remoteIntegration.version != currentVersion) {
            log.info("Remote config change detected! Old version: ${currentVersion}, New version: ${remoteIntegration.version}")
            this.currentVersion = remoteIntegration.version
            return true
        }

        return false
    }
}
