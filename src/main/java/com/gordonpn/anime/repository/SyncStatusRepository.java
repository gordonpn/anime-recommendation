package com.gordonpn.anime.repository;

import com.gordonpn.anime.entity.SyncStatusEntity;
import com.gordonpn.anime.models.ListStatusType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncStatusRepository extends JpaRepository<SyncStatusEntity, String> {
}