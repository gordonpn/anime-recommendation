package com.gordonpn.anime.controller;

import com.gordonpn.anime.repository.TokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    private final TokenRepository tokenRepository;

    @Value("${app.mode:cli}")
    private String appMode;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getShallowHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("mode", appMode);
        health.put("time", Instant.now().toString());
        return ResponseEntity.ok(health);
    }

    @GetMapping("/deep")
    public ResponseEntity<Map<String, Object>> getDeepHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        Map<String, Object> components = new LinkedHashMap<>();
        boolean overallUp = true;

        // 1. Check Database
        Map<String, Object> dbHealth = new HashMap<>();
        try {
            long count = tokenRepository.count();
            dbHealth.put("status", "UP");
            dbHealth.put("recordCount", count);
        } catch (Exception e) {
            dbHealth.put("status", "DOWN");
            dbHealth.put("error", e.getMessage());
            overallUp = false;
        }
        components.put("database", dbHealth);

        // 2. Check MAL Authentication status
        Map<String, Object> malHealth = new HashMap<>();
        try {
            com.gordonpn.anime.entity.TokenEntity latestToken = tokenRepository.findFirstByOrderByIdDesc();
            if (latestToken == null) {
                malHealth.put("status", "UP"); // Configured but auth hasn't happened yet
                malHealth.put("tokenStatus", "MISSING");
            } else if (latestToken.getExpiresAt() != null && latestToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                malHealth.put("status", "UP");
                malHealth.put("tokenStatus", "EXPIRED");
            } else {
                malHealth.put("status", "UP");
                malHealth.put("tokenStatus", "VALID");
            }
        } catch (Exception e) {
            malHealth.put("status", "DOWN");
            malHealth.put("error", e.getMessage());
            overallUp = false;
        }
        components.put("malAuth", malHealth);

        health.put("status", overallUp ? "UP" : "DOWN");
        health.put("mode", appMode);
        health.put("time", Instant.now().toString());
        health.put("components", components);

        if (overallUp) {
            return ResponseEntity.ok(health);
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }
}
