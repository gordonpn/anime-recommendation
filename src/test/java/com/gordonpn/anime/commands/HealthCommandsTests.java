package com.gordonpn.anime.commands;

import com.gordonpn.anime.entity.TokenEntity;
import com.gordonpn.anime.repository.TokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthCommandsTests {

    @Mock
    private TokenRepository tokenRepository;

    private HealthCommands healthCommands;

    @BeforeEach
    public void setUp() {
        healthCommands = new HealthCommands(tokenRepository);
        ReflectionTestUtils.setField(healthCommands, "appMode", "cli");
        ReflectionTestUtils.setField(healthCommands, "dbUrl", "jdbc:sqlite:test.db");
    }

    @Test
    public void testHealthReportValidToken() {
        TokenEntity mockToken = new TokenEntity();
        mockToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        when(tokenRepository.count()).thenReturn(1L);
        when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(mockToken);

        String result = healthCommands.health();

        assertTrue(result.contains("Runtime Mode: cli"));
        assertTrue(result.contains("Status: OK (Connected)"));
        assertTrue(result.contains("Token status: VALID"));
    }

    @Test
    public void testHealthReportMissingToken() {
        when(tokenRepository.count()).thenReturn(0L);
        when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(null);

        String result = healthCommands.health();

        assertTrue(result.contains("Token status: MISSING"));
    }
}
