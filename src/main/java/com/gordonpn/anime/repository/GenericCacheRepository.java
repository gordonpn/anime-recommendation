package com.gordonpn.anime.repository;

import com.gordonpn.anime.entity.GenericCacheEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GenericCacheRepository extends JpaRepository<GenericCacheEntity, String> {
}
