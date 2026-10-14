package catalog.adapter.out.persistence;

import catalog.adapter.out.persistence.entity.AchievementJpaEntity;
import catalog.adapter.out.persistence.mapper.AchievementMapper;
import catalog.adapter.out.persistence.repository.AchievementJpaRepository;
import catalog.application.port.out.AchievementRepositoryPort;
import catalog.domain.Achievement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AchievementPersistenceAdapter implements AchievementRepositoryPort {

    private final AchievementJpaRepository repository;

    public AchievementPersistenceAdapter(AchievementJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Achievement> findByGameId(long gameId) {
        return repository.findByGameId(gameId).stream().map(AchievementMapper::toDomain).toList();
    }

    /** Upsert by (gameId, steamAchievementId): rows that already exist keep their id and are updated. */
    @Override
    @Transactional
    public List<Achievement> saveAll(List<Achievement> achievements) {
        Map<Long, Map<String, Long>> existingIdsByGame = new HashMap<>();
        List<AchievementJpaEntity> entities = achievements.stream().map(a -> {
            Map<String, Long> existing = existingIdsByGame.computeIfAbsent(a.gameId(), gameId ->
                    repository.findByGameId(gameId).stream().collect(Collectors.toMap(
                            AchievementJpaEntity::getSteamAchievementId, AchievementJpaEntity::getId)));
            Long id = a.id() != null ? a.id() : existing.get(a.steamAchievementId());
            return AchievementMapper.toEntity(new Achievement(id, a.gameId(), a.steamAchievementId(),
                    a.name(), a.description(), a.iconUrl(), a.hidden(), a.updatedAt()));
        }).toList();
        return repository.saveAll(entities).stream().map(AchievementMapper::toDomain).toList();
    }
}
