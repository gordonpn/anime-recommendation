package com.gordonpn.anime.config;

import com.gordonpn.anime.models.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class AnimeClientConfig {

    @Value("${mal.client.id}")
    private String clientId;

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

        return RestClient.builder()
                .baseUrl("https://api.myanimelist.net/v2")
                .defaultHeader("X-MAL-CLIENT-ID", clientId)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptor(clientHttpRequestInterceptor)
                .build();
    }
}