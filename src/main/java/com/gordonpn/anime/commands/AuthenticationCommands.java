package com.gordonpn.anime.commands;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gordonpn.anime.config.AuthorizationState;
import com.gordonpn.anime.config.PKCEStateHolder;
import com.gordonpn.anime.handler.OAuth2ServerHandler;
import com.gordonpn.anime.models.PKCEPair;
import com.gordonpn.anime.models.TokenResponse;
import com.gordonpn.anime.repository.TokenRepository;
import com.gordonpn.anime.service.TokenExchangeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

import java.io.IOException;

@ShellComponent
@RequiredArgsConstructor
@Slf4j
public class AuthenticationCommands {
    private final AuthorizationState authorizationState;
    private final OAuth2ServerHandler oauth2ServerHandler;
    private final PKCEStateHolder pkceStateHolder;
    private final TokenExchangeService tokenExchangeService;
    private final TokenRepository tokenRepository;

    @ShellMethod(key = "login", value = "Authenticate via OAuth with MyAnimeList")
    public String login() throws IOException {
        String authCode = oauth2ServerHandler.waitForAuthorizationCode();
        if (authCode == null) {
            return "Authentication failed!";
        }

        String state = authorizationState.getState();
        PKCEPair pkcePair = pkceStateHolder.get(state);
        String codeVerifier = pkcePair.codeVerifier();
        TokenResponse tokens = tokenExchangeService.exchangeCode(authCode, codeVerifier);
        authorizationState.setTokenResponse(tokens);

        saveTokens(tokens);

        return "Successfully authenticated!";
    }

    @ShellMethod(key = "refresh", value = "Refresh the access token")
    public String refresh() throws JsonProcessingException {
        TokenResponse tokenResponse = authorizationState.getTokenResponse();
        String refreshToken = tokenResponse.getRefreshToken();
        TokenResponse tokens = tokenExchangeService.refreshToken(refreshToken);
        authorizationState.setTokenResponse(tokens);

        saveTokens(tokens);

        return "Successfully refreshed token!";
    }

    private void saveTokens(TokenResponse tokens) {
        log.info("Persisting tokens to database");
        tokenRepository.save(tokens);
    }
}
