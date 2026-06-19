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
        when(rankingService.calculateScore(anyDouble(), any(), any())).thenReturn(9.5);
        when(explanationService.generateExplanation(anyDouble(), anyList())).thenReturn("Because you liked Completed Show");

        // Execute
        List<RecommendationDetailedResult> results = recommendationFacade.getRecommendations("@me", 10);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(9.5, results.get(0).getScore());
        assertEquals("Because you liked Completed Show", results.get(0).getExplanation());
    }

    @Test
    public void testCompletedGroupingAndScoring() throws Exception {
        // Setup two completed shows: 101 (rated 9) and 102 (rated 7)
        // They are related (part of same component)
        UserAnimeListEntity c1 = new UserAnimeListEntity();
        c1.setAnimeId(101);
        c1.setStatus(ListStatusType.COMPLETED);
        c1.setUserScore(9);

        UserAnimeListEntity c2 = new UserAnimeListEntity();
        c2.setAnimeId(102);
        c2.setStatus(ListStatusType.COMPLETED);
        c2.setUserScore(7);

        when(userAnimeListRepository.findByStatus(ListStatusType.COMPLETED))
                .thenReturn(List.of(c1, c2));
        when(userAnimeListRepository.findByStatus(ListStatusType.DROPPED))
                .thenReturn(Collections.emptyList());

        // Mock details lookup for 101 (AOT Part 1)
        AnimeDetails details1 = new AnimeDetails();
        details1.setId(101);
        details1.setTitle("AOT Part 1");
        details1.setPopularity(200);
        
        RelatedAnime relTo102 = new RelatedAnime();
        Node n102 = new Node();
        n102.setId(102);
        relTo102.setNode(n102);
        details1.setRelatedAnime(List.of(relTo102));

        Recommendation rec = new Recommendation();
        rec.setNumRecommendations(4);
        Node recNode = new Node();
        recNode.setId(303);
        recNode.setTitle("Recommended Show");
        rec.setNode(recNode);
        details1.setRecommendations(List.of(rec));

        // Mock details lookup for 102 (AOT Part 2)
        AnimeDetails details2 = new AnimeDetails();
        details2.setId(102);
        details2.setTitle("AOT Part 2");
        details2.setPopularity(100); // More popular than part 1

        RelatedAnime relTo101 = new RelatedAnime();
        Node n101 = new Node();
        n101.setId(101);
        relTo101.setNode(n101);
        details2.setRelatedAnime(List.of(relTo101));
        details2.setRecommendations(Collections.emptyList());

        when(animeService.getAnimeById(101)).thenReturn(details1);
        when(animeService.getAnimeById(102)).thenReturn(details2);

        // Mock filtering
        when(filteringService.shouldInclude(eq(303), anySet(), anySet())).thenReturn(true);

        // Mock details lookup for recommended candidate 303
        AnimeDetails recDetails = new AnimeDetails();
        recDetails.setId(303);
        recDetails.setTitle("Recommended Show");
        recDetails.setMean(8.0);
        recDetails.setPopularity(500);

        // 303 has a related prequel 304 with rating 9.0
        RelatedAnime relTo304 = new RelatedAnime();
        relTo304.setRelationType("prequel");
        Node n304 = new Node();
        n304.setId(304);
        relTo304.setNode(n304);
        recDetails.setRelatedAnime(List.of(relTo304));

        AnimeDetails prequelDetails = new AnimeDetails();
        prequelDetails.setId(304);
        prequelDetails.setMean(9.0);

        when(animeService.getAnimeById(303)).thenReturn(recDetails);
        when(animeService.getAnimeById(304)).thenReturn(prequelDetails);

        when(rankingService.calculateScore(anyDouble(), any(), any())).thenReturn(12.0);
        when(explanationService.generateExplanation(anyDouble(), anyList())).thenReturn("Explained!");

        // Execute
        List<RecommendationDetailedResult> results = recommendationFacade.getRecommendations("@me", 10);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals("Recommended Show", results.get(0).getTitle());
        assertEquals(8.5, results.get(0).getMeanRating(), 0.01); // franchise-wide average: (8.0 + 9.0) / 2 = 8.5
        assertEquals(4.5714, results.get(0).getOccurrence(), 0.01); // weighted occurrences: 4 * (8.0 / 7.0) = 4.5714
        assertEquals(12.0, results.get(0).getScore());
    }
}
