package com.gordonpn.anime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.dto.TokenResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
public class TokenExchangeServiceTests {

    @Mock
    private RestClient oauthClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private ResponseEntity<String> responseEntity;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TokenExchangeService tokenExchangeService;

    @BeforeEach
    public void setUp() {
        tokenExchangeService = new TokenExchangeService(oauthClient, objectMapper);
        ReflectionTestUtils.setField(tokenExchangeService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(tokenExchangeService, "clientSecret", "test-secret");
        ReflectionTestUtils.setField(tokenExchangeService, "redirectUri", "http://test-redirect.uri");
    }

    @Test
    public void testExchangeCode() throws Exception {
        String jsonResponse = "{\"access_token\":\"acc123\",\"refresh_token\":\"ref456\"}";

        when(oauthClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(jsonResponse);

        TokenResponse result = tokenExchangeService.exchangeCode("code123", "verifier456");

        assertNotNull(result);
        verify(oauthClient, times(1)).post();
    }

    @Test
    public void testRefreshTokenNeeded() throws Exception {
        TokenResponse oldToken = new TokenResponse();
        oldToken.setRefreshToken("ref456");
        oldToken.setExpiresAt(LocalDateTime.now().minusMinutes(1)); // Expired

        String jsonResponse = "{\"access_token\":\"newAcc\",\"refresh_token\":\"newRef\"}";

        when(oauthClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.body(any(Object.class))).thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(jsonResponse);

        TokenResponse result = tokenExchangeService.refreshToken(oldToken);

        assertNotNull(result);
        verify(oauthClient, times(1)).post();
    }

    @Test
    public void testRefreshTokenNotNeeded() throws Exception {
        TokenResponse oldToken = new TokenResponse();
        oldToken.setExpiresAt(LocalDateTime.now().plusHours(1)); // Valid

        TokenResponse result = tokenExchangeService.refreshToken(oldToken);

        assertNotNull(result);
        verify(oauthClient, never()).post();
    }

    @Test
    public void testRefreshTokenNull() throws Exception {
        TokenResponse result = tokenExchangeService.refreshToken(null);
        assertNull(result);
    }
}
