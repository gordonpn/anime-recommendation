package com.gordonpn.anime.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MalAuthServiceTests {

    private MalAuthService malAuthService;

    @BeforeEach
    public void setUp() {
        malAuthService = new MalAuthService();
        ReflectionTestUtils.setField(malAuthService, "clientId", "test-client-id");
        ReflectionTestUtils.setField(malAuthService, "redirectUri", "http://test-redirect.uri");
    }

    @Test
    public void testBuildAuthorizationUrl() {
        URI uri = malAuthService.buildAuthorizationUrl("challenge123", "state456");
        String uriStr = uri.toString();

        assertTrue(uriStr.startsWith("https://myanimelist.net/v1/oauth2/authorize"));
        assertTrue(uriStr.contains("response_type=code"));
        assertTrue(uriStr.contains("client_id=test-client-id"));
        assertTrue(uriStr.contains("state=state456"));
        assertTrue(uriStr.contains("redirect_uri=http://test-redirect.uri"));
        assertTrue(uriStr.contains("code_challenge=challenge123"));
        assertTrue(uriStr.contains("code_challenge_method=plain"));
    }
}
