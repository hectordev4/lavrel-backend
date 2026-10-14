package catalog.adapter.out.persistence.repository;

import catalog.adapter.out.persistence.entity.GameMappingJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import shared.domain.Platform;

public interface GameMappingJpaRepository extends JpaRepository<GameMappingJpaEntity, Long> {

    Optional<GameMappingJpaEntity> findByPlatformAndPlatformGameId(Platform platform, String platformGameId);

    List<GameMappingJpaEntity> findByPlatformAndPlatformGameIdIn(Platform platform,
                                                                 Collection<String> platformGameIds);

    List<GameMappingJpaEntity> findByGameId(long gameId);

    /** A game may have several platform IDs on one platform (e.g. editions); take the oldest. */
    Optional<GameMappingJpaEntity> findFirstByGameIdAndPlatformOrderByIdAsc(long gameId, Platform platform);
}
