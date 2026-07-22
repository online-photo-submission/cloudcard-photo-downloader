package com.cloudcard.photoDownloader

import org.springframework.beans.BeansException
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.stereotype.Component

@Component
class SpringContextBridge implements ApplicationContextAware {
    private static ApplicationContext context;

    @Override
    void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    /**
     * This allows us to pull beans from the context into classes that can't be context-aware, like the logging appender.
     * @param beanClass The type of the component bean you want to retrieve
     * @return The Spring-managed component from the context that matches the specified class
     */
    static <T> T getBean(Class<T> beanClass) {
        return context.getBean(beanClass);
    }
}
