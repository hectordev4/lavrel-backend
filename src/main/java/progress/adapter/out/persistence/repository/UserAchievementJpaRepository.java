package progress.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import progress.adapter.out.persistence.entity.UserAchievementId;
import progress.adapter.out.persistence.entity.UserAchievementJpaEntity;

public interface UserAchievementJpaRepository extends JpaRepository<UserAchievementJpaEntity, UserAchievementId> {

    List<UserAchievementJpaEntity> findByIdUserId(long userId);

    /** @return 1 if inserted, 0 if the (user, achievement) row already existed */
    @Modifying
    @Query(value = """
            INSERT INTO user_achievement (user_id, achievement_id, source_account_id, unlocked_at, recorded_at)
            VALUES (:userId, :achievementId, :sourceAccountId, :unlockedAt, :recordedAt)
            ON CONFLICT (user_id, achievement_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("userId") long userId,
                       @Param("achievementId") long achievementId,
                       @Param("sourceAccountId") Long sourceAccountId,
                       @Param("unlockedAt") Instant unlockedAt,
                       @Param("recordedAt") Instant recordedAt);
}
