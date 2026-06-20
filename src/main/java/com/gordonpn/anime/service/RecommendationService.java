package com.gordonpn.anime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.AnimeDetails;
import com.gordonpn.anime.models.AnimeList;
import com.gordonpn.anime.models.Datum;
import com.gordonpn.anime.models.Node;
import com.gordonpn.anime.models.Recommendation;
import com.gordonpn.anime.models.RecommendationDetailedResult;
import com.gordonpn.anime.models.RecommendationResult;
import com.gordonpn.anime.models.userlist.Status;
import com.gordonpn.anime.service.recommendation.RecommendationFacade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    @Qualifier("objectMapper")
    private final ObjectMapper objectMapper;

    private final UserListService userListService;
    private final AnimeService animeService;
    private final RestClient apiKeyClient;
    private final RecommendationFacade recommendationFacade;

    public Map<String, Integer> recommendationOccurrence(String userName) {
        List<RecommendationDetailedResult> results = recommendationFacade.getRecommendations(userName, 50);
        Map<String, Integer> legacyMap = new LinkedHashMap<>();
        for (RecommendationDetailedResult res : results) {
            legacyMap.put(res.getTitle(), res.getOccurrence() != null ? (int) Math.round(res.getOccurrence()) : 0);
        }
        return legacyMap;
    }

    public AnimeList getSuggestions() {
        var body = apiKeyClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/anime/suggestions")
                        .queryParam("fields", "id,title")
                        .build())
                .retrieve()
                .toEntity(String.class)
                .getBody();

        log.debug("Suggestions response: {}", body);

        try {
            return objectMapper.readValue(body, AnimeList.class);
        } catch (JsonProcessingException e) {
            throw new com.gordonpn.anime.models.AnimeDataProcessingException("Failed to parse suggestions JSON", e);
        }
    }
}
