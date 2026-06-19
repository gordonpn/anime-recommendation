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
import java.io.InputStream;
import java.util.Properties;

@SpringBootApplication
@EnableScheduling
public class RecommendationApplication {

    public static void main(String[] args) {
        loadDotEnv();
        validateCredentials();

        SpringApplication app = new SpringApplication(RecommendationApplication.class);
        String mode = getAppMode();

        if ("cli".equalsIgnoreCase(mode)) {
            app.setWebApplicationType(WebApplicationType.NONE);
            System.setProperty("spring.shell.interactive.enabled", "true");
        } else if ("api".equalsIgnoreCase(mode)) {
            app.setWebApplicationType(WebApplicationType.SERVLET);
            System.setProperty("spring.shell.interactive.enabled", "false");
            System.setProperty("spring.autoconfigure.exclude",
                    "org.springframework.shell.boot.SpringShellAutoConfiguration," +
                    "org.springframework.shell.boot.JLineShellAutoConfiguration," +
                    "org.springframework.shell.boot.StandardAPIAutoConfiguration," +
                    "org.springframework.shell.boot.StandardCommandsAutoConfiguration," +
                    "org.springframework.shell.boot.LineReaderAutoConfiguration," +
                    "org.springframework.shell.boot.ShellRunnerAutoConfiguration");
        } else if ("both".equalsIgnoreCase(mode)) {
            app.setWebApplicationType(WebApplicationType.SERVLET);
            System.setProperty("spring.shell.interactive.enabled", "true");
        } else {
            throw new IllegalArgumentException("Unknown app.mode: " + mode);
        }

        ConfigurableApplicationContext context = app.run(args);
        context.registerShutdownHook();
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

    private static String getAppMode() {
        String mode = System.getProperty("app.mode");
        if (mode != null && !mode.isBlank()) {
            return mode.trim();
        }

        mode = System.getenv("APP_MODE");
        if (mode != null && !mode.isBlank()) {
            return mode.trim();
        }

        try (InputStream input = RecommendationApplication.class.getClassLoader()
                .getResourceAsStream("application.properties")) {
            if (input != null) {
                Properties prop = new Properties();
                prop.load(input);
                mode = prop.getProperty("app.mode");
                if (mode != null && !mode.isBlank()) {
                    return mode.trim();
                }
            }
        } catch (Exception ignored) {
        }

        return "cli"; // default
    }
}
