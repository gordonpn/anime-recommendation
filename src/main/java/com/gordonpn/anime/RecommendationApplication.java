package com.gordonpn.anime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

@SpringBootApplication
@EnableScheduling
public class RecommendationApplication {

    public static void main(String[] args) {
        loadDotEnv();
        validateCredentials();

        SpringApplication app = new SpringApplication(RecommendationApplication.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        System.setProperty("spring.shell.interactive.enabled", "true");

        ConfigurableApplicationContext context = app.run(args);
        context.close();
    }

    private static void loadDotEnv() {
        File envFile = new File(".env");
        if (envFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int idx = line.indexOf('=');
                    if (idx > 0) {
                        String key = line.substring(0, idx).trim();
                        String value = line.substring(idx + 1).trim();
                        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        } else if (value.startsWith("'") && value.endsWith("'") && value.length() >= 2) {
                            value = value.substring(1, value.length() - 1);
                        }
                        System.setProperty(key, value);
                    }
                }
            } catch (IOException e) {
                System.err.println("Warning: failed to read .env file: " + e.getMessage());
            }
        }
    }

    private static void validateCredentials() {
        String clientId = System.getProperty("MAL_CLIENT_ID");
        if (clientId == null) clientId = System.getenv("MAL_CLIENT_ID");

        String clientSecret = System.getProperty("MAL_CLIENT_SECRET");
        if (clientSecret == null) clientSecret = System.getenv("MAL_CLIENT_SECRET");

        if (clientId == null || clientId.isBlank() ||
                clientSecret == null || clientSecret.isBlank()) {
            throw new IllegalStateException("FAIL-FAST: Missing required MAL_CLIENT_ID or MAL_CLIENT_SECRET in environment or .env file.");
        }
    }
}
