package com.gordonpn.anime.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "tokens")
public class TokenResponse {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("access_token")
    @Column(name = "access_token", nullable = false)
    private String accessToken;

    @JsonProperty("refresh_token")
    @Column(name = "refresh_token", nullable = false)
    private String refreshToken;

    @JsonProperty("token_type")
    @Transient
    private String tokenType;

    @JsonProperty("expires_in")
    @Transient
    private Integer expiresIn;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @PrePersist
    @PreUpdate
    private void calculateExpiresAt() {
        if (expiresIn != null) {
            expiresAt = LocalDateTime.now().plusSeconds(expiresIn);
        }
    }
}
