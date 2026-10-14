package com.lavrel.progress.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_achievement")
public class UserAchievementJpaEntity {

    @EmbeddedId
    private UserAchievementId id;

    @Column(name = "source_account_id")
    private Long sourceAccountId;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    protected UserAchievementJpaEntity() {
    }

    public UserAchievementJpaEntity(UserAchievementId id, Long sourceAccountId, Instant unlockedAt, Instant recordedAt) {
        this.id = id;
        this.sourceAccountId = sourceAccountId;
        this.unlockedAt = unlockedAt;
        this.recordedAt = recordedAt;
    }

    public UserAchievementId getId() {
        return id;
    }

    public Long getSourceAccountId() {
        return sourceAccountId;
    }

    public Instant getUnlockedAt() {
        return unlockedAt;
    }

    public Instant getRecordedAt() {
        return recordedAt;
    }
}
