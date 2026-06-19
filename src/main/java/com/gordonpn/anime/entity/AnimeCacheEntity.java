package com.gordonpn.anime.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "anime_cache")
@Data
public class AnimeCacheEntity {

    @Id
    @Column(name = "anime_id")
    private Integer animeId;

    @Column(nullable = false)
    private String title;

    @Column(name = "details_json", nullable = false)
    private String detailsJson;

    @Column(name = "cached_at", nullable = false)
    private LocalDateTime cachedAt;
}
