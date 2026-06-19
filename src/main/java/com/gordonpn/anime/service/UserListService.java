package com.gordonpn.anime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.entity.GenericCacheEntity;
import com.gordonpn.anime.models.AnimeList;
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
                        uriBuilder.path("/users/{userName}/animelist")
                                .queryParam("sort", "list_score")
                                .queryParam("limit", MAX)
                                .queryParam("status", status.getType());

                        return uriBuilder.build(userName);
                    })
                    .retrieve()
                    .toEntity(String.class)
                    .getBody();

            AnimeList animeList = objectMapper.readValue(body, AnimeList.class);
            log.debug("User list fetched from MAL API and parsed successfully");

            // Save/Update to cache
            GenericCacheEntity cacheEntry = cachedOpt.orElse(new GenericCacheEntity());
            cacheEntry.setCacheKey(cacheKey);
            cacheEntry.setCacheValue(body);
            cacheEntry.setCachedAt(LocalDateTime.now());
            genericCacheRepository.save(cacheEntry);

            return animeList;
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
