package com.gordonpn.anime.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gordonpn.anime.models.AnimeList;
import com.gordonpn.anime.models.userlist.Status;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
public class UserListService {

    private static final int MAX = 1000;

    @Qualifier("objectMapper")
    private final ObjectMapper objectMapper;

    private final RestClient apiKeyClient;

    public AnimeList getListByUserName(String userName, Status status) throws JsonProcessingException {
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

        return objectMapper.readValue(body, AnimeList.class);
    }
}
