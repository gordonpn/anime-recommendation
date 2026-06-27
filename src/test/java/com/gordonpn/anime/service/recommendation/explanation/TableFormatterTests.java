package com.gordonpn.anime.service.recommendation.explanation;

import com.gordonpn.anime.models.Datum;
import com.gordonpn.anime.models.Node;
import com.gordonpn.anime.models.dto.RecommendationDetailedResult;
import com.gordonpn.anime.models.dto.SeasonalAnimeResult;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TableFormatterTests {

    private final TableFormatter tableFormatter = new TableFormatter();

    @Test
    public void testFormatRecommendations() {
        RecommendationDetailedResult res1 = RecommendationDetailedResult.builder()
                .id(1)
                .title("Frieren")
                .score(9.5)
                .occurrence(3.0)
                .meanRating(8.8)
                .explanation("Because you liked Steins;Gate")
                .build();

        RecommendationDetailedResult res2 = RecommendationDetailedResult.builder()
                .id(2)
                .title("A Really Long Title That Exceeds Forty Characters To Verify Truncation Logic")
                .score(7.2)
                .occurrence(1.0)
                .meanRating(7.5)
                .explanation("Because you liked Death Note")
                .build();

        String formatted = tableFormatter.formatRecommendations(List.of(res1, res2), "@me");

        assertNotNull(formatted);
        assertTrue(formatted.contains("=== Personalized Recommendations for @me ==="));
        assertTrue(formatted.contains("Frieren"));
        assertTrue(formatted.contains("9.50"));
        assertTrue(formatted.contains("3.0"));
        // Check truncation: title truncated at 37 chars + "..."
        assertTrue(formatted.contains("A Really Long Title That Exceeds Fort..."));
    }

    @Test
    public void testFormatSeasonal() {
        Set<SeasonalAnimeResult> sortedResult = new TreeSet<>(
                Comparator.<SeasonalAnimeResult>comparingDouble(a -> a.getMean() == null ? 0.0 : a.getMean()).reversed()
        );

        sortedResult.add(SeasonalAnimeResult.builder().id(1).title("Bleach TYBW").mean(9.1).build());
        sortedResult.add(SeasonalAnimeResult.builder().id(2).title("Bocchi the Rock").mean(8.5).build());

        String formatted = tableFormatter.formatSeasonal("5", sortedResult, 10);

        assertNotNull(formatted);
        assertTrue(formatted.contains("=== Top Seasonal Anime (Past 5 Years) ==="));
        assertTrue(formatted.contains("Bleach TYBW"));
        assertTrue(formatted.contains("Bocchi the Rock"));
        assertTrue(formatted.contains("9.10"));
        assertTrue(formatted.contains("8.50"));
    }

    @Test
    public void testFormatSuggestions() {
        Datum d1 = new Datum();
        Node n1 = new Node();
        n1.setId(101);
        n1.setTitle("Monster");
        d1.setNode(n1);

        Datum d2 = new Datum();
        Node n2 = new Node();
        n2.setId(102);
        n2.setTitle("Pluto");
        d2.setNode(n2);

        String formatted = tableFormatter.formatSuggestions(List.of(d1, d2), 10);

        assertNotNull(formatted);
        assertTrue(formatted.contains("=== Personalized Suggestions from MyAnimeList ==="));
        assertTrue(formatted.contains("Monster"));
        assertTrue(formatted.contains("Pluto"));
        assertTrue(formatted.contains("101"));
        assertTrue(formatted.contains("102"));
    }
}
