package progress.adapter.out.persistence.mapper;

import progress.adapter.out.persistence.entity.UserAchievementJpaEntity;
import progress.domain.UserAchievement;
import progress.adapter.out.persistence.entity.UserAchievementId;

public final class UserAchievementMapper {

    private UserAchievementMapper() {
    }

    public static UserAchievement toDomain(UserAchievementJpaEntity e) {
        return new UserAchievement(e.getId().userId(), e.getId().achievementId(), e.getSourceAccountId(),
                e.getUnlockedAt(), e.getRecordedAt());
    }

    public static UserAchievementJpaEntity toEntity(UserAchievement d) {
        return new UserAchievementJpaEntity(new UserAchievementId(d.userId(), d.achievementId()), d.sourceAccountId(),
                d.unlockedAt(), d.recordedAt());
    }
}
