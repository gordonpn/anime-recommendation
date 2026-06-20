package com.gordonpn.anime.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.*;
import com.gordonpn.anime.models.dto.RecommendationDetailedResult;
import com.gordonpn.anime.service.recommendation.RecommendationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RecommendationServiceTests {

    @Mock
    private UserListService userListService;

    @Mock
    private AnimeService animeService;

    @Mock
    private RestClient apiKeyClient;

    @Mock
    private RecommendationFacade recommendationFacade;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private RecommendationService recommendationService;

    @BeforeEach
    public void setUp() {
        recommendationService = new RecommendationService(objectMapper, userListService, animeService, apiKeyClient, recommendationFacade);
    }

    @Test
    public void testRecommendationOccurrence() throws Exception {
        RecommendationDetailedResult detailed = RecommendationDetailedResult.builder()
                .id(3)
                .title("Recommended Anime")
                .score(9.5)
                .occurrence(5.0)
                .explanation("Test explanation")
                .build();

        when(recommendationFacade.getRecommendations("testUser", 50)).thenReturn(List.of(detailed));

        // Run
        Map<String, Integer> result = recommendationService.recommendationOccurrence("testUser");

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(5, result.get("Recommended Anime"));
    }
}
