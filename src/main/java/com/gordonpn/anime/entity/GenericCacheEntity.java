package com.gordonpn.anime.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "generic_cache")
@Data
public class GenericCacheEntity {

    @Id
    @Column(name = "cache_key", nullable = false)
    private String cacheKey;

    @Column(name = "cache_value", nullable = false)
    private String cacheValue;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;
}
