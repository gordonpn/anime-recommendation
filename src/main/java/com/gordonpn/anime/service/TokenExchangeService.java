package com.gordonpn.anime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenExchangeService {
    private final RestClient oauthClient;
    @Qualifier("objectMapper")
    private final ObjectMapper objectMapper;
    @Value("${mal.client.id}")
    private String clientId;
    @Value("${mal.client.secret}")
    private String clientSecret;
    @Value("${mal.redirect.uri}")
    private String redirectUri;

    public TokenResponse exchangeCode(String authCode, String codeVerifier) throws JsonProcessingException {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "authorization_code");
        formData.add("code", authCode);
        formData.add("redirect_uri", redirectUri);
        formData.add("code_verifier", codeVerifier);

        String body = oauthClient.post()
                .uri("/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toEntity(String.class)
                .getBody();

        return objectMapper.readValue(body, TokenResponse.class);
    }

    public TokenResponse refreshToken(TokenResponse tokenResponse) throws JsonProcessingException {
        if (tokenResponse == null) {
            log.info("No token to refresh");
            return null;
        }

        if (tokenResponse.getExpiresAt() == null) {
            log.info("Token missing expiry date, cannot refresh");
            return tokenResponse;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime bufferTime = now.plusMinutes(5);

        if (tokenResponse.getExpiresAt().isAfter(bufferTime)) {
            log.info("Token still valid until {}, no refresh needed", tokenResponse.getExpiresAt());
            return tokenResponse;
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", tokenResponse.getRefreshToken());

        String body = oauthClient.post()
                .uri("/v1/oauth2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .toEntity(String.class)
                .getBody();

        return objectMapper.readValue(body, TokenResponse.class);
    }
}
