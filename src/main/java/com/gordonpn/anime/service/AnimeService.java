package com.gordonpn.anime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.AnimeDetails;
import com.gordonpn.anime.models.SeasonalAnime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimeService {

    @Qualifier("objectMapper")
    private final ObjectMapper objectMapper;

    private final RestClient apiKeyClient;

    public AnimeDetails getAnimeById(int animeId) throws JsonProcessingException {
        String body = apiKeyClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/anime/{animeId}")
                        .queryParam("fields", "id,recommendations")
                        .build(animeId))
                .retrieve()
                .toEntity(String.class)
                .getBody();

        AnimeDetails animeDetails = objectMapper.readValue(body, AnimeDetails.class);
        log.info("Anime details fetched: {}", animeDetails.title);
        return animeDetails;
    }

    public SeasonalAnime getSeasonalAnime(String season, int year) throws JsonProcessingException {
        var body = apiKeyClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/anime/season/{year}/{season}")
                        .queryParam("fields", "id")
                        .queryParam("limit", "500")
                        .queryParam("sort", "anime_score")
                        .build(year, season))
                .retrieve()
                .toEntity(String.class)
                .getBody();

        log.info("Seasonal anime: {}", body);

        SeasonalAnime seasonalAnime = objectMapper.readValue(body, SeasonalAnime.class);
        log.info("Seasonal anime fetched: {}", seasonalAnime);
        return seasonalAnime;
    }
}
