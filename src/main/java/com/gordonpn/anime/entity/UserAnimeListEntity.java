package com.gordonpn.anime.entity;

import com.gordonpn.anime.models.ListStatusType;
import com.gordonpn.anime.models.ListStatusTypeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "user_anime_list")
@Data
public class UserAnimeListEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "anime_id", nullable = false)
    private Integer animeId;

    @Convert(converter = ListStatusTypeConverter.class)
    @Column(nullable = false)
    private ListStatusType status;

    @Column(name = "user_score")
    private Integer userScore;
}