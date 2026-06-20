package com.gordonpn.anime.util;

import com.gordonpn.anime.models.dto.PKCEPair;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PKCEUtil {
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder().withoutPadding();

    private static String generateCodeVerifier() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return base64Encoder.encodeToString(randomBytes);
    }

    private static String generateCodeChallenge(String codeVerifier) {
        return codeVerifier;
    }

    public PKCEPair generatePKCEPair() {
        String codeVerifier = generateCodeVerifier();
        String codeChallenge = generateCodeChallenge(codeVerifier);
        return new PKCEPair(codeVerifier, codeChallenge);
    }
}
