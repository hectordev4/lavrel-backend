package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.adapter.out.persistence.mapper.AchievementMappingMapper;
import com.lavrel.catalog.adapter.out.persistence.repository.AchievementMappingJpaRepository;
import com.lavrel.catalog.application.port.out.AchievementMappingRepositoryPort;
import com.lavrel.catalog.domain.AchievementMapping;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.shared.domain.Platform;

@Component
public class AchievementMappingPersistenceAdapter implements AchievementMappingRepositoryPort {

    private final AchievementMappingJpaRepository repository;

    public AchievementMappingPersistenceAdapter(AchievementMappingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AchievementMapping> findByGameAndPlatform(long gameId, Platform platform,
                                                          Collection<String> platformAchievementIds) {
        if (platformAchievementIds.isEmpty()) {
            return List.of();
        }
        return repository.findByGameAndPlatform(gameId, platform, platformAchievementIds).stream()
                .map(AchievementMappingMapper::toDomain).toList();
    }

    /** The composite key (achievementId, platform) makes saveAll an upsert. */
    @Override
    @Transactional
    public List<AchievementMapping> saveAll(List<AchievementMapping> mappings) {
        return repository.saveAll(mappings.stream().map(AchievementMappingMapper::toEntity).toList())
                .stream().map(AchievementMappingMapper::toDomain).toList();
    }
}
