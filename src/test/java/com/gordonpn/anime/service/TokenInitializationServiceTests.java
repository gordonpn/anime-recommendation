package com.gordonpn.anime.service;

import com.gordonpn.anime.config.AuthorizationState;
import com.gordonpn.anime.models.TokenResponse;
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
        TokenResponse mockToken = new TokenResponse();
        mockToken.setExpiresAt(LocalDateTime.now().plusHours(1));

        when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(mockToken);

        tokenInitializationService.initializeTokens();

        verify(authorizationState, times(1)).setTokenResponse(mockToken);
    }

    @Test
    public void testInitializeTokensExpiredToken() {
        TokenResponse mockToken = new TokenResponse();
        mockToken.setExpiresAt(LocalDateTime.now().minusHours(1));

        when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(mockToken);

        tokenInitializationService.initializeTokens();

        verify(authorizationState, times(1)).setTokenResponse(mockToken);
    }

    @Test
    public void testInitializeTokensNoToken() {
        when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(null);

        tokenInitializationService.initializeTokens();

        verify(authorizationState, never()).setTokenResponse(any());
    }
}
