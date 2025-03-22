package com.gordonpn.anime.repository;

import com.gordonpn.anime.models.TokenResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<TokenResponse, Long> {
    TokenResponse findFirstByOrderByIdDesc();
}
