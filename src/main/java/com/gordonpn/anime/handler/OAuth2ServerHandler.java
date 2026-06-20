package com.gordonpn.anime.handler;

import com.gordonpn.anime.config.AuthorizationState;
import com.gordonpn.anime.config.PKCEStateHolder;
import com.gordonpn.anime.models.dto.PKCEPair;
import com.gordonpn.anime.service.MalAuthService;
import com.gordonpn.anime.util.Browser;
import com.gordonpn.anime.util.CallbackHandler;
import com.gordonpn.anime.util.PKCEUtil;
import com.gordonpn.anime.util.RandomState;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;

@Component
@RequiredArgsConstructor
public class OAuth2ServerHandler {
    private final PKCEStateHolder pkceStateHolder;
    private final MalAuthService malAuthService;
    private final PKCEUtil pkceUtil;
    private final AuthorizationState authorizationState;
    @Value("${mal.callback.port}")
    private String port;

    public String waitForAuthorizationCode() throws IOException {
        String authorizationCode;

        try (ServerSocket serverSocket = new ServerSocket(Integer.parseInt(port))) {

            String state = RandomState.generate();
            PKCEPair pkcePair = pkceUtil.generatePKCEPair();
            pkceStateHolder.put(state, pkcePair);

            String codeChallenge = pkcePair.codeChallenge();
            Browser.launch(malAuthService.buildAuthorizationUrl(codeChallenge, state));

            authorizationCode = CallbackHandler.extractCodeFromCallback(serverSocket, state);
            authorizationState.setAuthorizationCode(authorizationCode);
            authorizationState.setState(state);
        }
        return authorizationCode;
    }
}
