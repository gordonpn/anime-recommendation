package com.gordonpn.anime.config;

import com.gordonpn.anime.models.TokenResponse;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
@NoArgsConstructor
public class AuthorizationState {

    private String state;

    private String authorizationCode;

    private TokenResponse tokenResponse;
}
