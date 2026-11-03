package com.lavrel.catalog.adapter.out.persistence.mapper;

import com.lavrel.catalog.adapter.out.persistence.entity.AchievementMappingJpaEntity;
import com.lavrel.catalog.domain.AchievementMapping;
import com.lavrel.catalog.adapter.out.persistence.entity.AchievementMappingId;

public final class AchievementMappingMapper {

    private AchievementMappingMapper() {
    }

    public static AchievementMapping toDomain(AchievementMappingJpaEntity e) {
        return new AchievementMapping(e.getId().achievementId(), e.getId().platform(),
                e.getPlatformAchievementId(), e.isAvailable());
    }

    public static AchievementMappingJpaEntity toEntity(AchievementMapping d) {
        return new AchievementMappingJpaEntity(new AchievementMappingId(d.achievementId(), d.platform()),
                d.platformAchievementId(), d.available());
    }
}
