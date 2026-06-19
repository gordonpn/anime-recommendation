package com.gordonpn.anime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.AnimeDetails;
import com.gordonpn.anime.models.SeasonalAnime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AnimeServiceTests {

    @Mock
    private RestClient apiKeyClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    @Mock
    private ResponseEntity<String> responseEntity;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AnimeService animeService;

    @BeforeEach
    public void setUp() {
        animeService = new AnimeService(objectMapper, apiKeyClient);
    }

    @Test
    public void testGetAnimeById() throws Exception {
        String jsonResponse = "{\"id\":123,\"title\":\"Test Anime\"}";

        when(apiKeyClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(jsonResponse);

        AnimeDetails result = animeService.getAnimeById(123);

        assertNotNull(result);
        verify(apiKeyClient, times(1)).get();
    }

    @Test
    public void testGetSeasonalAnime() throws Exception {
        String jsonResponse = "{\"data\":[]}";

        when(apiKeyClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(Function.class))).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.toEntity(String.class)).thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn(jsonResponse);

        SeasonalAnime result = animeService.getSeasonalAnime("winter", 2026);

        assertNotNull(result);
        verify(apiKeyClient, times(1)).get();
    }
}
