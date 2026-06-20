package com.gordonpn.anime.service;

import com.gordonpn.anime.config.AuthorizationState;
import com.gordonpn.anime.entity.TokenEntity;
import com.gordonpn.anime.models.dto.TokenResponse;
import com.gordonpn.anime.repository.TokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TokenInitializationServiceTests {

    @Mock
    private AuthorizationState authorizationState;

    @Mock
    private TokenRepository tokenRepository;

    @InjectMocks
    private TokenInitializationService tokenInitializationService;

    @Test
    public void testInitializeTokensValidToken() {
        TokenEntity mockEntity = new TokenEntity();
        mockEntity.setAccessToken("access");
        mockEntity.setRefreshToken("refresh");
        mockEntity.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(mockEntity);

        tokenInitializationService.initializeTokens();

        TokenResponse expectedToken = new TokenResponse();
        expectedToken.setAccessToken("access");
        expectedToken.setRefreshToken("refresh");
        expectedToken.setExpiresAt(mockEntity.getExpiresAt());

        verify(authorizationState, times(1)).setTokenResponse(expectedToken);
    }

    @Test
    public void testInitializeTokensExpiredToken() {
        TokenEntity mockEntity = new TokenEntity();
        mockEntity.setAccessToken("access");
        mockEntity.setRefreshToken("refresh");
        mockEntity.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(mockEntity);

        tokenInitializationService.initializeTokens();

        TokenResponse expectedToken = new TokenResponse();
        expectedToken.setAccessToken("access");
        expectedToken.setRefreshToken("refresh");
        expectedToken.setExpiresAt(mockEntity.getExpiresAt());

        verify(authorizationState, times(1)).setTokenResponse(expectedToken);
    }

    @Test
    public void testInitializeTokensNoToken() {
        when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(null);

        tokenInitializationService.initializeTokens();

        verify(authorizationState, never()).setTokenResponse(any());
    }
}
