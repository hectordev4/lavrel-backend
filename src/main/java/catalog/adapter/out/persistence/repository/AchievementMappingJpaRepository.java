package catalog.adapter.out.persistence.repository;

import catalog.adapter.out.persistence.entity.AchievementMappingJpaEntity;
import java.util.Optional;
import java.util.List;
import shared.domain.Platform;
import catalog.adapter.out.persistence.entity.AchievementMappingId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementMappingJpaRepository extends JpaRepository<AchievementMappingJpaEntity, AchievementMappingId> {

    List<AchievementMappingJpaEntity> findByIdAchievementId(long achievementId);

    Optional<AchievementMappingJpaEntity> findByIdPlatformAndPlatformAchievementId(Platform platform, String platformAchievementId);
}
