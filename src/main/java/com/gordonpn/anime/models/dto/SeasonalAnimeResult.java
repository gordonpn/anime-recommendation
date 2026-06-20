package com.gordonpn.anime.models.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SeasonalAnimeResult {
    public Double mean;
    private String title;
    private Integer id;
}
