package com.gordonpn.anime.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.*;
import com.gordonpn.anime.service.AnimeService;
import com.gordonpn.anime.service.RecommendationService;
import com.gordonpn.anime.service.recommendation.RecommendationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.jline.reader.LineReader;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RecommendationCommandsTests {

    @Mock
    private RecommendationService recommendationService;
    @Mock
    private AnimeService animeService;
    @Mock
    private RecommendationFacade recommendationFacade;

    private RecommendationCommands recommendationCommands;

    @BeforeEach
    public void setUp() {
        recommendationCommands = new RecommendationCommands(
                new ObjectMapper(), // Use real objectMapper for formatting output
                recommendationService,
                animeService,
                recommendationFacade,
                null
        );
    }

    @Test
    public void testSeasonalGrouping() throws Exception {
        // Prepare mock seasonal anime list payload
        SeasonalAnime seasonalAnime = new SeasonalAnime();
        List<Datum> seasonalData = new ArrayList<>();

        Datum datum1 = new Datum();
        Node node1 = new Node();
        node1.setId(1);
        node1.setTitle("Attack on Titan Season 4 Part 1");
        node1.setMean(9.0);
        datum1.setNode(node1);
        seasonalData.add(datum1);

        Datum datum2 = new Datum();
        Node node2 = new Node();
        node2.setId(2);
        node2.setTitle("Attack on Titan Season 4 Part 2");
        node2.setMean(8.8);
        datum2.setNode(node2);
        seasonalData.add(datum2);

        seasonalAnime.setData(seasonalData);

        // Mock getSeasonalAnime calls
        when(animeService.getSeasonalAnime(anyString(), anyInt())).thenReturn(seasonalAnime);

        // Mock details lookup for node 1
        AnimeDetails details1 = new AnimeDetails();
        details1.setId(1);
        details1.setTitle("Attack on Titan Season 4 Part 1");
        details1.setMean(9.0);
        details1.setPopularity(100);
        
        RelatedAnime relTo2 = new RelatedAnime();
        Node relNode2 = new Node();
        relNode2.setId(2);
        relNode2.setTitle("Attack on Titan Season 4 Part 2");
        relTo2.setNode(relNode2);
        details1.setRelatedAnime(List.of(relTo2));

        // Mock details lookup for node 2
        AnimeDetails details2 = new AnimeDetails();
        details2.setId(2);
        details2.setTitle("Attack on Titan Season 4 Part 2");
        details2.setMean(8.8);
        details2.setPopularity(50); // More popular than part 1 (rank 50 < 100)

        RelatedAnime relTo1 = new RelatedAnime();
        Node relNode1 = new Node();
        relNode1.setId(1);
        relNode1.setTitle("Attack on Titan Season 4 Part 1");
        relTo1.setNode(relNode1);
        details2.setRelatedAnime(List.of(relTo1));

        when(animeService.getAnimeById(1)).thenReturn(details1);
        when(animeService.getAnimeById(2)).thenReturn(details2);

        // Run (limit 5) - should group them under the representative title ("Attack on Titan Season 4 Part 2" due to popularity 50 vs 100)
        // Combined rating: (9.0 + 8.8) / 2 = 8.90
        String output = recommendationCommands.seasonal("1", false, 5);

        assertNotNull(output);
        assertTrue(output.contains("Attack on Titan Season 4 Part 2"));
        assertTrue(output.contains("8.90"));
    }

    @Mock
    private ObjectProvider<LineReader> lineReaderProviderMock;
    @Mock
    private LineReader lineReaderMock;

    @Test
    public void testRecommendationInteractivePrompts() throws Exception {
        recommendationCommands = new RecommendationCommands(
                new ObjectMapper(),
                recommendationService,
                animeService,
                recommendationFacade,
                lineReaderProviderMock
        );

        when(lineReaderProviderMock.getIfAvailable()).thenReturn(lineReaderMock);
        // Returns empty username (defaults to @me), "y" for JSON (maps to true), "5" for limit,
        // and empty strings for optional min/max years and decayMonths (defaults to null)
        when(lineReaderMock.readLine(anyString()))
                .thenReturn("")
                .thenReturn("y")
                .thenReturn("5")
                .thenReturn("")
                .thenReturn("")
                .thenReturn("");

        List<RecommendationDetailedResult> recs = List.of(
                RecommendationDetailedResult.builder().id(12).title("Test").score(10.0).build()
        );
        when(recommendationFacade.getRecommendations("@me", 5, null, null, null)).thenReturn(recs);

        String result = recommendationCommands.recommendation(null, null, null, null, null, null);

        assertNotNull(result);
        assertTrue(result.contains("\"id\":12"));
        assertTrue(result.contains("\"title\":\"Test\""));
    }
}
