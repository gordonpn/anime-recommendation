package com.gordonpn.anime.config;

import com.gordonpn.anime.models.TokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
public class AnimeClientConfig {

    @Value("${mal.client.id}")
    private String clientId;

    @Value("${mal.retry.max-attempts:3}")
    private int maxAttempts;

    @Value("${mal.retry.initial-backoff-ms:500}")
    private long initialBackoffMs;

    @Value("${mal.retry.backoff-multiplier:2.0}")
    private double backoffMultiplier;

    @Bean
    public RestClient oauthClient() {
        return RestClient.builder()
                .baseUrl("https://myanimelist.net")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public RestClient apiKeyClient(AuthorizationState authorizationState) {
        ClientHttpRequestInterceptor clientHttpRequestInterceptor = (HttpRequest request, byte[] body, ClientHttpRequestExecution execution) -> {
            TokenResponse tokenResponse = authorizationState.getTokenResponse();
            if (tokenResponse == null) {
                return execution.execute(request, body);
            }
            String accessToken = tokenResponse.getAccessToken();
            if (accessToken != null && !accessToken.isEmpty()) {
                request.getHeaders().setBearerAuth(accessToken);
            }
            return execution.execute(request, body);
        };

        log.info("Configuring apiKeyClient with maxAttempts={}, initialBackoffMs={}, backoffMultiplier={}",
                maxAttempts, initialBackoffMs, backoffMultiplier);

        return RestClient.builder()
                .baseUrl("https://api.myanimelist.net/v2")
                .defaultHeader("X-MAL-CLIENT-ID", clientId)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(clientHttpRequestInterceptor)
                .requestInterceptor(new MalRetryInterceptor(maxAttempts, initialBackoffMs, backoffMultiplier))
                .build();
    }
}