package com.cloudcard.photoDownloader

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.GenericBeanDefinition
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.MapPropertySource

class RemoteConfigInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final Logger log = LoggerFactory.getLogger(RemoteConfigInitializer.class)

    @Override
    void initialize(ConfigurableApplicationContext context) {
        ConfigurableEnvironment env = context.environment

        String pat = env.getProperty("cloudcard.api.accessToken")
        String apiUrl = env.getProperty("cloudcard.api.url")
        String integrationName = env.getProperty("cloudcard.integration.name")
        RemoteConfigService remoteConfigService = new RemoteConfigService(pat: pat, apiUrl: apiUrl, integrationName: integrationName)

        log.info("Initializing RemoteConfigInitializer with API URL: " + apiUrl + ", Integration Name: " + integrationName)

        Integration integration = remoteConfigService.fetchRemoteConfig()

        // set the version on startup to establish a baseline
        remoteConfigService.currentVersion = integration.version

        Map<String, Object> remoteProperties = integration.integrationConfigs.collectEntries { [(it.propertyName): it.propertyValue] }

        env.getPropertySources().addFirst(new MapPropertySource("remoteApiConfig", remoteProperties))

        // Register the RemoteConfigService instance as a bean, so the DownloaderService (which @Autowires RemoteConfigService) gets this specific instance.
        GenericBeanDefinition beanDefinition = new GenericBeanDefinition()
        beanDefinition.setBeanClass(RemoteConfigService)
        beanDefinition.setInstanceSupplier { remoteConfigService }
        beanDefinition.setAutowireCandidate(true)
        ((BeanDefinitionRegistry) context.getBeanFactory()).registerBeanDefinition("remoteConfigService", beanDefinition)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Integration {
    String name
    Integer version
    String integrationType
    List<IntegrationConfig> integrationConfigs
}

@JsonIgnoreProperties(ignoreUnknown = true)
class IntegrationConfig {
    String propertyName
    String propertyValue
}