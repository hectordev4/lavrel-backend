package com.lavrel.catalog.adapter.out.persistence.mapper;

import com.lavrel.catalog.adapter.out.persistence.entity.AchievementJpaEntity;
import com.lavrel.catalog.domain.Achievement;

public final class AchievementMapper {

    private AchievementMapper() {
    }

    public static Achievement toDomain(AchievementJpaEntity e) {
        return new Achievement(e.getId(), e.getGameId(), e.getSteamAchievementId(), e.getName(),
                e.getDescription(), e.getIconUrl(), e.isHidden(), e.getUpdatedAt());
    }

    public static AchievementJpaEntity toEntity(Achievement d) {
        return new AchievementJpaEntity(d.id(), d.gameId(), d.steamAchievementId(), d.name(),
                d.description(), d.iconUrl(), d.hidden(), d.updatedAt());
    }
}
