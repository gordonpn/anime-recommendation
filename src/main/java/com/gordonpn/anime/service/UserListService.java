package com.gordonpn.anime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.entity.GenericCacheEntity;
import com.gordonpn.anime.models.AnimeList;
import com.gordonpn.anime.models.MalResourceNotFoundException;
import com.gordonpn.anime.models.userlist.Status;
import com.gordonpn.anime.repository.GenericCacheRepository;
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
public class UserListService {

    private static final int MAX = 1000;

    @Qualifier("objectMapper")
    private final ObjectMapper objectMapper;

    private final RestClient apiKeyClient;
    private final GenericCacheRepository genericCacheRepository;

    public AnimeList getListByUserName(String userName, Status status) throws JsonProcessingException {
        String cacheKey = "userlist:" + userName + ":" + status.getType();
        Optional<GenericCacheEntity> cachedOpt = genericCacheRepository.findById(cacheKey);

        if (cachedOpt.isPresent()) {
            GenericCacheEntity cached = cachedOpt.get();
            if (cached.getCachedAt().isAfter(LocalDateTime.now().minusDays(1))) {
                log.debug("Cache hit for user list key: {}", cacheKey);
                return objectMapper.readValue(cached.getCacheValue(), AnimeList.class);
            }
            log.info("Cache expired for user list key: {}", cacheKey);
        }

        log.info("Cache miss or expired for user list key: {}. Fetching from MAL", cacheKey);
        try {
            String body = apiKeyClient.get()
                    .uri(uriBuilder -> {
                        java.net.URI uri = uriBuilder.path("/users/{userName}/animelist")
                                .queryParam("sort", "list_score")
                                .queryParam("limit", MAX)
                                .queryParam("status", status.getType())
                                .build(userName);

                        return org.springframework.web.util.UriComponentsBuilder.fromUri(uri)
                                .queryParam("fields", "list_status%7Bscore%7D,alternative_titles")
                                .build(true)
                                .toUri();
                    })
                    .retrieve()
                    .toEntity(String.class)
                    .getBody();

            if (body == null || body.trim().startsWith("<")) {
                throw new RuntimeException("MAL API returned an HTML page (likely a rate-limit or captcha challenge) instead of a JSON payload.");
            }

            AnimeList animeList = objectMapper.readValue(body, AnimeList.class);
            log.debug("User list fetched from MAL API and parsed successfully");

            // Save/Update to cache
            GenericCacheEntity cacheEntry = cachedOpt.orElse(new GenericCacheEntity());
            cacheEntry.setCacheKey(cacheKey);
            cacheEntry.setCacheValue(body);
            cacheEntry.setCachedAt(LocalDateTime.now());
            genericCacheRepository.save(cacheEntry);

            return animeList;
        } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
            log.error("User {} not found on MyAnimeList (404 Not Found): {}", userName, e.getMessage());
            throw new MalResourceNotFoundException("User " + userName + " not found on MyAnimeList", e);
        } catch (Exception e) {
            log.error("Failed to fetch or parse user list key {} from MAL: {}", cacheKey, e.getMessage());
            if (cachedOpt.isPresent()) {
                log.warn("Falling back to expired cache for user list key: {}", cacheKey);
                try {
                    return objectMapper.readValue(cachedOpt.get().getCacheValue(), AnimeList.class);
                } catch (Exception ex) {
                    log.error("Failed to parse expired cache for user list key {}: {}", cacheKey, ex.getMessage());
                }
            }
            throw e;
        }
    }
}
