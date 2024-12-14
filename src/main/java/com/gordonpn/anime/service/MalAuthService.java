package com.gordonpn.anime.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MalAuthService {

    @Value("${mal.client.id}")
    private String clientId;

    @Value("${mal.redirect.uri}")
    private String redirectUri;

    public URI buildAuthorizationUrl(String codeChallenge, String state) {
        return UriComponentsBuilder
                .fromUriString("https://myanimelist.net/v1/oauth2/authorize")
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("state", state)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("code_challenge", codeChallenge)
                .queryParam("code_challenge_method", "plain")
                .build()
                .toUri();
    }
}
