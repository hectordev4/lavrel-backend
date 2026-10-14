package com.lavrel.catalog.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import com.lavrel.shared.domain.Platform;
import java.io.Serializable;

@Embeddable
public record AchievementMappingId(
        @Column(name = "achievement_id", nullable = false) long achievementId,
        @Enumerated(EnumType.STRING) @Column(name = "platform", nullable = false) Platform platform) implements Serializable {
}
