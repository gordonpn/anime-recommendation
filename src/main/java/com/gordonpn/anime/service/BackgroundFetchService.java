package com.gordonpn.anime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gordonpn.anime.config.AuthorizationState;
import com.gordonpn.anime.entity.SyncStatusEntity;
import com.gordonpn.anime.entity.UserAnimeListEntity;
import com.gordonpn.anime.models.AnimeList;
import com.gordonpn.anime.models.Datum;
import com.gordonpn.anime.models.ListStatusType;
import com.gordonpn.anime.models.TokenResponse;
import com.gordonpn.anime.models.userlist.Status;
import com.gordonpn.anime.repository.SyncStatusRepository;
import com.gordonpn.anime.repository.TokenRepository;
import com.gordonpn.anime.repository.UserAnimeListRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class BackgroundFetchService {
    private static final long ONE_HOUR_IN_MILLIS = 60 * 60 * 1000L;
    private final UserListService userListService;
    private final AuthorizationState authorizationState;
    private final TokenExchangeService tokenExchangeService;
    private final TokenRepository tokenRepository;
    private final UserAnimeListRepository userAnimeListRepository;
    private final SyncStatusRepository syncStatusRepository;

    @Scheduled(fixedDelay = ONE_HOUR_IN_MILLIS)
    public void fetchList() throws JsonProcessingException {
        TokenResponse tokenResponse = authorizationState.getTokenResponse();
        TokenResponse tokens = tokenExchangeService.refreshToken(tokenResponse);
        authorizationState.setTokenResponse(tokens);
        tokenRepository.save(tokens);

        for (Status status : Status.values()) {
            log.info("Fetching anime list for status: {}", status.getType());
            AnimeList listByUserName = userListService.getListByUserName("@me", status);
            if (listByUserName == null || listByUserName.getData() == null) {
                log.warn("No anime list data to save");
                continue;
            }
            ListStatusType listStatusType = ListStatusType.fromValue(status.getType());
            log.info("Converting status from {} to ListStatusType {}", status.getType(), listStatusType);
            for (Datum datum : listByUserName.getData()) {
                log.info("Processing anime: {}. Status: {}", datum.toString(), listStatusType);
                UserAnimeListEntity entity = new UserAnimeListEntity();
                entity.setAnimeId(datum.getNode().getId());
                entity.setStatus(listStatusType);
//                entity.setUserScore(datum.getListStatus().getScore());

                userAnimeListRepository.save(entity);
                log.info("Saved anime into user list table: {}", datum.getNode().getTitle());

            }
            LocalDateTime now = LocalDateTime.now();
            SyncStatusEntity syncStatus = syncStatusRepository.findById(listStatusType.getValue())
                    .orElse(new SyncStatusEntity());
            syncStatus.setListType(listStatusType.getValue());
            syncStatus.setLastSyncedAt(now);
            syncStatusRepository.save(syncStatus);
            log.info("Updated sync status timestamp for: {}", listStatusType);
            log.info("Saved {} anime entries", listByUserName.getData().size());
        }

    }

    public void fetchAnimeAnAnime() {

    }
}
