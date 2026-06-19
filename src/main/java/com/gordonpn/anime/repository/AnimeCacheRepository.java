package com.gordonpn.anime.repository;

import com.gordonpn.anime.entity.AnimeCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnimeCacheRepository extends JpaRepository<AnimeCacheEntity, Integer> {
}
