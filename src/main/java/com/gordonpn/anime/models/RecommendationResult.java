package com.gordonpn.anime.models;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecommendationResult {
    private String title;
    private Integer id;
}
