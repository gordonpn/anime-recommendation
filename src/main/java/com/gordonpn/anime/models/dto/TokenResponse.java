package com.gordonpn.anime.models.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private Integer expiresIn;

    private LocalDateTime expiresAt;

    public void calculateExpiresAt() {
        if (expiresIn != null) {
            expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
        }
    }
}
