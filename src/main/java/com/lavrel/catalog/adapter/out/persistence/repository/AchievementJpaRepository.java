package com.lavrel.catalog.adapter.out.persistence.repository;

import com.lavrel.catalog.adapter.out.persistence.entity.AchievementJpaEntity;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementJpaRepository extends JpaRepository<AchievementJpaEntity, Long> {

    List<AchievementJpaEntity> findByGameId(long gameId);

    Optional<AchievementJpaEntity> findByGameIdAndSteamAchievementId(long gameId, String steamAchievementId);
}
