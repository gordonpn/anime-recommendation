package com.gordonpn.anime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.entity.AnimeCacheEntity;
import com.gordonpn.anime.models.AnimeDetails;
import com.gordonpn.anime.models.SeasonalAnime;
import com.gordonpn.anime.repository.AnimeCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimeService {

    @Qualifier("objectMapper")
    private final ObjectMapper objectMapper;

    private final RestClient apiKeyClient;
    private final AnimeCacheRepository animeCacheRepository;

    public AnimeDetails getAnimeById(int animeId) throws JsonProcessingException {
        Optional<AnimeCacheEntity> cachedOpt = animeCacheRepository.findById(animeId);
        if (cachedOpt.isPresent()) {
            AnimeCacheEntity cached = cachedOpt.get();
            if (cached.getCachedAt().isAfter(LocalDateTime.now().minusDays(7))) {
                log.debug("Cache hit for anime ID: {}", animeId);
                return objectMapper.readValue(cached.getDetailsJson(), AnimeDetails.class);
            }
            log.info("Cache expired for anime ID: {}", animeId);
        }

        log.info("Cache miss or expired for anime ID: {}. Fetching from MAL...", animeId);
        String body;
        try {
            body = apiKeyClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/anime/{animeId}")
                            .queryParam("fields", "id,title,recommendations,mean,popularity,related_anime")
                            .build(animeId))
                    .retrieve()
                    .toEntity(String.class)
                    .getBody();
        } catch (Exception e) {
            log.error("Failed to fetch anime ID {} from MAL: {}", animeId, e.getMessage());
            if (cachedOpt.isPresent()) {
                log.warn("Falling back to expired cache for anime ID: {}", animeId);
                return objectMapper.readValue(cachedOpt.get().getDetailsJson(), AnimeDetails.class);
            }
            throw e;
        }

        AnimeDetails animeDetails = objectMapper.readValue(body, AnimeDetails.class);
        log.info("Anime details fetched: {}", animeDetails.title);

        // Save/Update to cache
        AnimeCacheEntity cacheEntry = cachedOpt.orElse(new AnimeCacheEntity());
        cacheEntry.setAnimeId(animeId);
        cacheEntry.setTitle(animeDetails.getTitle() != null ? animeDetails.getTitle() : "Unknown");
        cacheEntry.setDetailsJson(body);
        cacheEntry.setCachedAt(LocalDateTime.now());
        animeCacheRepository.save(cacheEntry);

        return animeDetails;
    }

    public SeasonalAnime getSeasonalAnime(String season, int year) throws JsonProcessingException {
        var body = apiKeyClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/anime/season/{year}/{season}")
                        .queryParam("fields", "id,title,mean")
                        .queryParam("limit", "500")
                        .queryParam("sort", "anime_score")
                        .build(year, season))
                .retrieve()
                .toEntity(String.class)
                .getBody();

        log.debug("Seasonal anime: {}", body);

        SeasonalAnime seasonalAnime = objectMapper.readValue(body, SeasonalAnime.class);
        log.debug("Seasonal anime fetched: {}", seasonalAnime);
        return seasonalAnime;
    }
}
