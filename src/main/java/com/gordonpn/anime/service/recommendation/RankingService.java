package com.gordonpn.anime.service.recommendation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RankingService {

    @Value("${recommendation.weights.occurrence:1.0}")
    private double weightOccurrence;

    @Value("${recommendation.weights.rating:0.5}")
    private double weightRating;

    @Value("${recommendation.weights.popularity:0.0001}")
    private double weightPopularity;

    public double calculateScore(double occurrence, Double meanRating, Integer popularity) {
        double occScore = occurrence;
        double ratingScore = (meanRating != null) ? meanRating : 0.0;
        
        // Normalize popularity rank (lower is better, e.g. #1 gets ~10.0, #10000 gets ~5.0)
        double popScore = 0.0;
        if (popularity != null && popularity > 0) {
            popScore = (10000.0 / (10000.0 + popularity)) * 10.0;
        }

        return (weightOccurrence * occScore) + (weightRating * ratingScore) + (weightPopularity * popScore);
    }
}
