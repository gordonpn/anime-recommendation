package com.gordonpn.anime.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.sqlite.SQLiteDataSource;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class FlywayConfig {

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @Bean
    public Flyway flyway(DataSource dataSource) {
        log.info("Initializing Flyway");
        log.info("Database URL from properties: {}", dbUrl);

        if (dataSource instanceof SQLiteDataSource sqliteDS) {
            log.info("SQLite DataSource URL: {}", sqliteDS.getUrl());
        }

        Flyway flyway = Flyway.configure()
                .dataSource(dbUrl, null, null)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        try {
            flyway.migrate();
            log.info("Flyway migration completed successfully");
        } catch (Exception e) {
            log.error("Flyway migration failed!", e);
            throw e;
        }

        return flyway;
    }

    @PostConstruct
    public void logCurrentDirectory() {
        String currentDir = System.getProperty("user.dir");
        log.info("Current working directory: {}", currentDir);
    }
}
