package com.gordonpn.anime.service.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gordonpn.anime.entity.AnimeCacheEntity;
import com.gordonpn.anime.entity.UserAnimeListEntity;
import com.gordonpn.anime.models.*;
import com.gordonpn.anime.models.userlist.Status;
import com.gordonpn.anime.repository.AnimeCacheRepository;
import com.gordonpn.anime.repository.UserAnimeListRepository;
import com.gordonpn.anime.service.AnimeService;
import com.gordonpn.anime.service.UserListService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationFacade {

    private final UserAnimeListRepository userAnimeListRepository;
    private final AnimeCacheRepository animeCacheRepository;
    private final UserListService userListService;
    private final AnimeService animeService;
    private final FilteringService filteringService;
    private final RankingService rankingService;
    private final ExplanationService explanationService;

    public List<RecommendationDetailedResult> getRecommendations(String userName, int limit) throws JsonProcessingException {
        List<Integer> completedIds = new ArrayList<>();
        Map<Integer, String> completedIdToTitleMap = new HashMap<>();
        Set<Integer> droppedIds = new HashSet<>();

        // 1. Gather completed & dropped lists
        if ("@me".equalsIgnoreCase(userName)) {
            log.info("Retrieving list from local SQLite database for @me");
            List<UserAnimeListEntity> completedLocal = userAnimeListRepository.findByStatus(ListStatusType.COMPLETED);
            List<UserAnimeListEntity> droppedLocal = userAnimeListRepository.findByStatus(ListStatusType.DROPPED);

            for (UserAnimeListEntity entity : completedLocal) {
                completedIds.add(entity.getAnimeId());
                String title = animeCacheRepository.findById(entity.getAnimeId())
                        .map(AnimeCacheEntity::getTitle)
                        .orElse("Unknown Anime (ID: " + entity.getAnimeId() + ")");
                completedIdToTitleMap.put(entity.getAnimeId(), title);
            }
            for (UserAnimeListEntity entity : droppedLocal) {
                droppedIds.add(entity.getAnimeId());
            }
        } else {
            log.info("Fetching list live from MAL API for user: {}", userName);
            AnimeList completedList = userListService.getListByUserName(userName, Status.COMPLETED);
            AnimeList droppedList = userListService.getListByUserName(userName, Status.DROPPED);

            if (completedList != null && completedList.getData() != null) {
                for (Datum datum : completedList.getData()) {
                    Integer id = datum.getNode().getId();
                    completedIds.add(id);
                    completedIdToTitleMap.put(id, datum.getNode().getTitle());
                }
            }
            if (droppedList != null && droppedList.getData() != null) {
                for (Datum datum : droppedList.getData()) {
                    droppedIds.add(datum.getNode().getId());
                }
            }
        }

        // 2. Fetch completed anime details & extract candidate recommendations
        Map<Integer, Integer> recOccurrenceMap = new HashMap<>();
        Map<Integer, List<String>> recommendationSourcesMap = new HashMap<>();
        Set<Integer> completedIdsSet = new HashSet<>(completedIds);

        for (Integer completedId : completedIds) {
            try {
                AnimeDetails details = animeService.getAnimeById(completedId);
                String completedTitle = details.getTitle() != null ? details.getTitle() : completedIdToTitleMap.getOrDefault(completedId, "Unknown");
                
                // Update map to have the actual fetched title in case it was unknown
                completedIdToTitleMap.put(completedId, completedTitle);

                List<Recommendation> recommendations = details.getRecommendations();
                if (recommendations == null) {
                    continue;
                }

                for (Recommendation recommendation : recommendations) {
                    Node recNode = recommendation.getNode();
                    if (recNode == null || recNode.getId() == null) {
                        continue;
                    }
                    Integer recNodeId = recNode.getId();

                    if (filteringService.shouldInclude(recNodeId, completedIdsSet, droppedIds)) {
                        int numRecs = recommendation.getNumRecommendations() != null ? recommendation.getNumRecommendations() : 1;
                        recOccurrenceMap.merge(recNodeId, numRecs, Integer::sum);
                        recommendationSourcesMap.computeIfAbsent(recNodeId, _ -> new ArrayList<>())
                                .add(completedTitle);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to process recommendations for completed anime ID: {}", completedId, e);
            }
        }

        // 3. Sort candidates by recommendation count and take top ones for details query
        List<Integer> topCandidateIds = recOccurrenceMap.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(Map.Entry::getKey)
                .limit(Math.max(100, limit * 2))
                .collect(Collectors.toList());

        List<RecommendationDetailedResult> detailedResults = new ArrayList<>();

        // 4. Fetch details for top candidates and rank them
        for (Integer candidateId : topCandidateIds) {
            try {
                AnimeDetails details = animeService.getAnimeById(candidateId);
                int occurrence = recOccurrenceMap.getOrDefault(candidateId, 0);
                
                double score = rankingService.calculateScore(occurrence, details.getMean(), details.getPopularity());
                String explanation = explanationService.generateExplanation(occurrence, recommendationSourcesMap.get(candidateId));

                detailedResults.add(RecommendationDetailedResult.builder()
                        .id(candidateId)
                        .title(details.getTitle())
                        .score(score)
                        .occurrence(occurrence)
                        .meanRating(details.getMean())
                        .popularity(details.getPopularity())
                        .explanation(explanation)
                        .build());
            } catch (Exception e) {
                log.error("Failed to rank candidate ID: {}", candidateId, e);
            }
        }

        // 5. Final sorting by score descending and return
        return detailedResults.stream()
                .sorted((r1, r2) -> Double.compare(r2.getScore(), r1.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
