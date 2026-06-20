package com.gordonpn.anime.repository;

import com.gordonpn.anime.entity.TokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<TokenEntity, Long> {
    TokenEntity findFirstByOrderByIdDesc();
}
