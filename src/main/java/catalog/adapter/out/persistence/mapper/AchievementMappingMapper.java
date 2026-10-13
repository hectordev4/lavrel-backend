package catalog.adapter.out.persistence.mapper;

import catalog.adapter.out.persistence.entity.AchievementMappingJpaEntity;
import catalog.domain.AchievementMapping;
import catalog.adapter.out.persistence.entity.AchievementMappingId;

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
