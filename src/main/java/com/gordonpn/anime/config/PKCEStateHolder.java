package com.gordonpn.anime.config;

import com.gordonpn.anime.models.dto.PKCEPair;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@NoArgsConstructor
public class PKCEStateHolder {
    private final Map<String, PKCEPair> statePKCEMap = new ConcurrentHashMap<>();

    public void put(String state, PKCEPair pkce) {
        statePKCEMap.put(state, pkce);
    }

    public PKCEPair get(String state) {
        return statePKCEMap.get(state);
    }

    public void remove(String state) {
        statePKCEMap.remove(state);
    }
}
