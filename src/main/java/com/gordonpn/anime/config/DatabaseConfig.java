package com.gordonpn.anime.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class DatabaseConfig {
    @Value("${spring.datasource.url}")
    private String dbUrl;

    @PostConstruct
    public void verifyDatabaseCreation() {
        String dbPath = dbUrl.replace("jdbc:sqlite:", "");
        File dbFile = new File(dbPath);

        log.info("Database path: {}", dbFile.getAbsolutePath());
        log.info("Database exists: {}", dbFile.exists());
        log.info("Database can write: {}", dbFile.canWrite());

        try (Connection conn = DriverManager.getConnection(dbUrl)) {
            log.info("Successfully connected to database: {}", conn.getMetaData());
        } catch (SQLException e) {
            log.error("Failed to connect to database", e);
        }
    }
}
