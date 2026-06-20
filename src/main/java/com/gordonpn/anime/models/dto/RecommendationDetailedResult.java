package com.gordonpn.anime.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationDetailedResult {
    private Integer id;
    private String title;
    private Double score;
    private Double occurrence;
    private Double meanRating;
    private Integer popularity;
    private String explanation;
}
