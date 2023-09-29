package com.cloudcard.photoDownloader;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(value = "db.datasource.enabled", havingValue = "true")
public class DatabaseConfig {

    @Value("${db.datasource.driverClassName}")
    String driverClassName;
    @Value("${db.datasource.url}")
    String url;
    @Value("${db.datasource.username}")
    String username;
    @Value("${db.datasource.password}")
    String password;
    @Value("${db.datasource.schema:}")
    String schemaName;

    @Bean
    public DataSource getDataSource() {

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        if (!schemaName.isEmpty()) dataSource.setSchema(schemaName);
        return dataSource;

    }

    @Bean
    public JdbcTemplate getJdbcTemplate(DataSource dataSource) {

        return new JdbcTemplate(dataSource);
    }

}
