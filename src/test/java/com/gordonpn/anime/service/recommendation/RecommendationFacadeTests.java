package com.gordonpn.anime.service.recommendation;

import com.gordonpn.anime.entity.UserAnimeListEntity;
import com.gordonpn.anime.models.*;
import com.gordonpn.anime.models.userlist.Status;
import com.gordonpn.anime.repository.AnimeCacheRepository;
import com.gordonpn.anime.repository.UserAnimeListRepository;
import com.gordonpn.anime.service.AnimeService;
import com.gordonpn.anime.service.UserListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RecommendationFacadeTests {

    @Mock private UserAnimeListRepository userAnimeListRepository;
    @Mock private AnimeCacheRepository animeCacheRepository;
    @Mock private UserListService userListService;
    @Mock private AnimeService animeService;
    @Mock private FilteringService filteringService;
    @Mock private RankingService rankingService;
    @Mock private ExplanationService explanationService;

    private RecommendationFacade recommendationFacade;

    @BeforeEach
    public void setUp() {
        recommendationFacade = new RecommendationFacade(
                userAnimeListRepository,
                animeCacheRepository,
                userListService,
                animeService,
                filteringService,
                rankingService,
                explanationService
        );
    }

    @Test
    public void testGetRecommendationsLocal() throws Exception {
        // Prepare local list for @me
        UserAnimeListEntity completedEntity = new UserAnimeListEntity();
        completedEntity.setAnimeId(101);
        completedEntity.setStatus(ListStatusType.COMPLETED);

        when(userAnimeListRepository.findByStatus(ListStatusType.COMPLETED))
                .thenReturn(List.of(completedEntity));
        when(userAnimeListRepository.findByStatus(ListStatusType.DROPPED))
                .thenReturn(Collections.emptyList());

        // Mock details lookup for completed anime
        AnimeDetails completedDetails = new AnimeDetails();
        completedDetails.setId(101);
        completedDetails.setTitle("Completed Show");
        
        Recommendation rec = new Recommendation();
        rec.setNumRecommendations(5);
        Node recNode = new Node();
        recNode.setId(202);
        recNode.setTitle("Recommended Show");
        rec.setNode(recNode);
        completedDetails.setRecommendations(List.of(rec));

        when(animeService.getAnimeById(101)).thenReturn(completedDetails);

        // Mock filtering
        when(filteringService.shouldInclude(eq(202), anySet(), anySet())).thenReturn(true);

        // Mock details lookup for recommended candidate
        AnimeDetails recDetails = new AnimeDetails();
        recDetails.setId(202);
        recDetails.setTitle("Recommended Show");
        recDetails.setMean(8.5);
        recDetails.setPopularity(500);

        when(animeService.getAnimeById(202)).thenReturn(recDetails);

        // Mock ranking & explanation
        when(rankingService.calculateScore(5, 8.5, 500)).thenReturn(9.5);
        when(explanationService.generateExplanation(eq(5), anyList())).thenReturn("Because you liked Completed Show");

        // Execute
        List<RecommendationDetailedResult> results = recommendationFacade.getRecommendations("@me", 10);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Recommended Show", results.get(0).getTitle());
        assertEquals(9.5, results.get(0).getScore());
        assertEquals("Because you liked Completed Show", results.get(0).getExplanation());
    }
}
