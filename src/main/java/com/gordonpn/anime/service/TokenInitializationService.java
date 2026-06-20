package com.gordonpn.anime.service;

import com.gordonpn.anime.config.AuthorizationState;
import com.gordonpn.anime.entity.TokenEntity;
import com.gordonpn.anime.models.dto.TokenResponse;
import com.gordonpn.anime.repository.TokenRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenInitializationService {
    private final AuthorizationState authorizationState;
    private final TokenRepository tokenRepository;

    @PostConstruct
    public void initializeTokens() {
        log.info("Looking for existing tokens in database");
        TokenEntity latestEntity = tokenRepository.findFirstByOrderByIdDesc();

        if (latestEntity != null) {
            TokenResponse latestToken = new TokenResponse();
            latestToken.setAccessToken(latestEntity.getAccessToken());
            latestToken.setRefreshToken(latestEntity.getRefreshToken());
            latestToken.setExpiresAt(latestEntity.getExpiresAt());

            if (latestToken.getExpiresAt() != null && latestToken.getExpiresAt().isAfter(LocalDateTime.now())) {
                log.info("Found valid token, loading into application state");
                authorizationState.setTokenResponse(latestToken);
            } else {
                log.info("Found expired token, will need to refresh");
                authorizationState.setTokenResponse(latestToken);
                // Could auto-refresh here
            }
        } else {
            log.info("No tokens found in database");
        }
    }
}
