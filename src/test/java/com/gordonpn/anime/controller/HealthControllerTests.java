package com.gordonpn.anime.controller;

import com.gordonpn.anime.models.TokenResponse;
import com.gordonpn.anime.repository.TokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "app.mode=api")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public class HealthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TokenRepository tokenRepository;

    @Test
    public void testShallowHealth() throws Exception {
        mockMvc.perform(get("/api/v1/health")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.mode", is("api")));
    }

    @Test
    public void testDeepHealthValidToken() throws Exception {
        TokenResponse mockToken = new TokenResponse();
        mockToken.setAccessToken("valid-token");
        mockToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        Mockito.when(tokenRepository.count()).thenReturn(1L);
        Mockito.when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(mockToken);

        mockMvc.perform(get("/api/v1/health/deep")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components.database.status", is("UP")))
                .andExpect(jsonPath("$.components.database.recordCount", is(1)))
                .andExpect(jsonPath("$.components.malAuth.tokenStatus", is("VALID")));
    }

    @Test
    public void testDeepHealthExpiredToken() throws Exception {
        TokenResponse mockToken = new TokenResponse();
        mockToken.setAccessToken("expired-token");
        mockToken.setExpiresAt(LocalDateTime.now().minusDays(1));

        Mockito.when(tokenRepository.count()).thenReturn(1L);
        Mockito.when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(mockToken);

        mockMvc.perform(get("/api/v1/health/deep")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components.database.status", is("UP")))
                .andExpect(jsonPath("$.components.malAuth.tokenStatus", is("EXPIRED")));
    }

    @Test
    public void testDeepHealthMissingToken() throws Exception {
        Mockito.when(tokenRepository.count()).thenReturn(0L);
        Mockito.when(tokenRepository.findFirstByOrderByIdDesc()).thenReturn(null);

        mockMvc.perform(get("/api/v1/health/deep")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.components.database.status", is("UP")))
                .andExpect(jsonPath("$.components.malAuth.tokenStatus", is("MISSING")));
    }

    @Test
    public void testDeepHealthDatabaseDown() throws Exception {
        Mockito.when(tokenRepository.count()).thenThrow(new RuntimeException("Database timeout"));

        mockMvc.perform(get("/api/v1/health/deep")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status", is("DOWN")))
                .andExpect(jsonPath("$.components.database.status", is("DOWN")))
                .andExpect(jsonPath("$.components.database.error", is("Database timeout")));
    }
}
