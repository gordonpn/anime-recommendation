package com.gordonpn.anime.entity;

import com.gordonpn.anime.models.ListStatusType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "sync_status")
@Data
public class SyncStatusEntity {
    @Id
    @Column(name = "list_type")
    private String listType;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;

    public ListStatusType getListTypeEnum() {
        return ListStatusType.fromValue(listType);
    }

    public void setListTypeEnum(ListStatusType type) {
        this.listType = type.getValue();
    }
}