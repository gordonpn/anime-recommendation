package com.gordonpn.anime.repository;

import com.gordonpn.anime.entity.UserAnimeListEntity;
import com.gordonpn.anime.models.ListStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAnimeListRepository extends JpaRepository<UserAnimeListEntity, Integer> {

    List<UserAnimeListEntity> findByStatus(ListStatusType status);

    Optional<UserAnimeListEntity> findByAnimeId(Integer animeId);

    List<UserAnimeListEntity> findAll();

    void deleteByAnimeId(Integer animeId);
}