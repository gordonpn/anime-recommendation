package com.gordonpn.anime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.AnimeDetails;
import com.gordonpn.anime.models.AnimeList;
import com.gordonpn.anime.models.Datum;
import com.gordonpn.anime.models.Node;
import com.gordonpn.anime.models.Recommendation;
import com.gordonpn.anime.models.RecommendationResult;
import com.gordonpn.anime.models.userlist.Status;
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

    public Map<String, Integer> recommendationOccurrence(String userName) throws JsonProcessingException {
        HashMap<RecommendationResult, Integer> compiledRecommendations = new HashMap<>();

        AnimeList completedList = userListService.getListByUserName(userName, Status.COMPLETED);
        AnimeList droppedList = userListService.getListByUserName(userName, Status.DROPPED);

        List<Datum> completedListData = completedList.getData();
        List<Datum> droppedListData = droppedList.getData();
        HashSet<Integer> existingIds = new HashSet<>();

        for (Datum anime : completedListData) {
            Node node = anime.getNode();
            Integer id = node.getId();
            existingIds.add(id);
        }

        for (Datum anime : droppedListData) {
            Node node = anime.getNode();
            Integer id = node.getId();
            existingIds.add(id);
        }

        for (Datum anime : completedListData) {
            try {
                Node animeNode = anime.getNode();
                Integer animeNodeId = animeNode.getId();
                AnimeDetails details = animeService.getAnimeById(animeNodeId);

                List<Recommendation> recommendations = details.getRecommendations();
                for (Recommendation recommendation : recommendations) {
                    Integer numRecommendations = recommendation.getNumRecommendations();
                    Node recommendationNode = recommendation.getNode();
                    Integer recommendationNodeId = recommendationNode.getId();

                    if (!existingIds.contains(recommendationNodeId)) {
                        RecommendationResult recommendationResult = RecommendationResult.builder()
                                .id(recommendationNodeId)
                                .title(recommendationNode.getTitle())
                                .build();
                        compiledRecommendations.merge(recommendationResult, numRecommendations, Integer::sum);
                    }
                }
            } catch (Exception e) {
                log.error("Error fetching anime by ID", e);
            }
        }

        return compiledRecommendations.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .collect(Collectors.toMap(
                        recommendationResultIntegerEntry -> recommendationResultIntegerEntry.getKey().getTitle(),
                        Map.Entry::getValue,
                        (e1, _) -> e1,
                        LinkedHashMap::new
                ));
    }

    public void getSuggestions() throws JsonProcessingException {
        var body = apiKeyClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/anime/suggestions")
                        .queryParam("fields", "id")
                        .build())
                .retrieve()
                .toEntity(String.class)
                .getBody();

        log.info("Suggestions: {}", body);

        AnimeDetails animeDetails = objectMapper.readValue(body, AnimeDetails.class);
        log.info("Anime details fetched: {}", animeDetails.title);
    }
}
