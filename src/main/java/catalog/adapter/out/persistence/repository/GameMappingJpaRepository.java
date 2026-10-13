package catalog.adapter.out.persistence.repository;

import catalog.adapter.out.persistence.entity.GameMappingJpaEntity;
import java.util.Optional;
import java.util.List;
import shared.domain.Platform;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameMappingJpaRepository extends JpaRepository<GameMappingJpaEntity, Long> {

    Optional<GameMappingJpaEntity> findByPlatformAndPlatformGameId(Platform platform, String platformGameId);

    List<GameMappingJpaEntity> findByPlatformAndPlatformGameIdIn(Platform platform, Collection<String> platformGameIds);

    List<GameMappingJpaEntity> findByGameId(long gameId);
}
