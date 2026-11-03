package com.lavrel.catalog.adapter.out.persistence.repository;

import com.lavrel.catalog.adapter.out.persistence.entity.AchievementMappingId;
import com.lavrel.catalog.adapter.out.persistence.entity.AchievementMappingJpaEntity;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.lavrel.shared.domain.Platform;

public interface AchievementMappingJpaRepository
        extends JpaRepository<AchievementMappingJpaEntity, AchievementMappingId> {

    List<AchievementMappingJpaEntity> findByIdAchievementId(long achievementId);

    /**
     * Joined on the achievement's game, because some platforms reuse achievement IDs across games.
     * (Replaces the earlier findByIdPlatformAndPlatformAchievementId, which was ambiguous.)
     */
    @Query("""
            select m from AchievementMappingJpaEntity m, AchievementJpaEntity a
            where m.id.achievementId = a.id
              and a.gameId = :gameId
              and m.id.platform = :platform
              and m.platformAchievementId in :platformAchievementIds
            """)
    List<AchievementMappingJpaEntity> findByGameAndPlatform(
            @Param("gameId") long gameId,
            @Param("platform") Platform platform,
            @Param("platformAchievementIds") Collection<String> platformAchievementIds);
}
