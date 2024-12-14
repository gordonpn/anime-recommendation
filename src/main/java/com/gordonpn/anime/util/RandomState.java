package com.gordonpn.anime.util;

import java.security.SecureRandom;
import java.util.Base64;

public final class RandomState {
    private RandomState() {
    }

    public static String generate() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
