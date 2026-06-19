package com.gordonpn.anime.service.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RankingServiceTests {

    private RankingService rankingService;

    @BeforeEach
    public void setUp() {
        rankingService = new RankingService();
        ReflectionTestUtils.setField(rankingService, "weightOccurrence", 1.0);
        ReflectionTestUtils.setField(rankingService, "weightRating", 0.5);
        ReflectionTestUtils.setField(rankingService, "weightPopularity", 0.1);
    }

    @Test
    public void testCalculateScore() {
        // formula: (w_occ * occ) + (w_rating * rating) + (w_pop * pop_score)
        // occurrence = 5
        // rating = 8.0
        // popularity = 1000 -> pop_score = (10000 / 11000) * 10 = 9.090909
        // expected = (1.0 * 5) + (0.5 * 8.0) + (0.1 * 9.090909) = 5.0 + 4.0 + 0.9090909 = 9.9090909
        double score = rankingService.calculateScore(5, 8.0, 1000);
        assertEquals(9.9090909, score, 0.0001);
    }

    @Test
    public void testCalculateScoreNullValues() {
        double score = rankingService.calculateScore(3, null, null);
        assertEquals(3.0, score, 0.0001);
    }
}
