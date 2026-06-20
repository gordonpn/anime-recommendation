package com.gordonpn.anime.commands;

import com.gordonpn.anime.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.time.LocalDateTime;

@ShellComponent
@RequiredArgsConstructor
public class HealthCommands {

    private final TokenRepository tokenRepository;

    @Value("${app.mode:cli}")
    private String appMode;

    @Value("${spring.datasource.url}")
    private String dbUrl;

    @ShellMethod(key = "health", value = "Check CLI, database, and MyAnimeList authentication health status")
    public String health() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Recommendation CLI Health Diagnostics ===\n");
        sb.append(String.format("Runtime Mode: %s\n", appMode));
        sb.append(String.format("Current Time: %s\n", java.time.Instant.now().toString()));
        sb.append("\n");

        // 1. Check SQLite Database
        sb.append("[Database status]\n");
        try {
            long count = tokenRepository.count();
            sb.append("  Status: OK (Connected)\n");
            sb.append(String.format("  SQLite DB URL: %s\n", dbUrl));
            sb.append(String.format("  Token records in DB: %d\n", count));
        } catch (Exception e) {
            sb.append(String.format("  Status: ERROR (%s)\n", e.getMessage()));
        }
        sb.append("\n");

        // 2. Check MAL credentials and authentication status
        sb.append("[MyAnimeList Authorization status]\n");
        try {
            com.gordonpn.anime.entity.TokenEntity latestToken = tokenRepository.findFirstByOrderByIdDesc();
            if (latestToken == null) {
                sb.append("  Token status: MISSING (Please run 'login' to authenticate)\n");
            } else if (latestToken.getExpiresAt() != null && latestToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                sb.append(String.format("  Token status: EXPIRED (Expired at: %s - Run 'refresh' to renew)\n", latestToken.getExpiresAt()));
            } else {
                sb.append("  Token status: VALID\n");
                if (latestToken.getExpiresAt() != null) {
                    sb.append(String.format("  Expires at: %s\n", latestToken.getExpiresAt()));
                }
            }
        } catch (Exception e) {
            sb.append(String.format("  Auth status: UNKNOWN (Error reading token: %s)\n", e.getMessage()));
        }

        sb.append("=============================================");
        return sb.toString();
    }
}
