package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.adapter.out.persistence.mapper.GameMappingMapper;
import com.lavrel.catalog.adapter.out.persistence.repository.GameMappingJpaRepository;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.domain.GameMapping;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.shared.domain.Platform;

@Component
public class GameMappingPersistenceAdapter implements GameMappingRepositoryPort {

    private final GameMappingJpaRepository repository;

    public GameMappingPersistenceAdapter(GameMappingJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GameMapping> findByPlatformAndPlatformGameIds(Platform platform,
                                                              Collection<String> platformGameIds) {
        if (platformGameIds.isEmpty()) {
            return List.of();
        }
        return repository.findByPlatformAndPlatformGameIdIn(platform, platformGameIds).stream()
                .map(GameMappingMapper::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GameMapping> findByGameIdAndPlatform(long gameId, Platform platform) {
        return repository.findFirstByGameIdAndPlatformOrderByIdAsc(gameId, platform)
                .map(GameMappingMapper::toDomain);
    }

    @Override
    @Transactional
    public GameMapping save(GameMapping mapping) {
        return GameMappingMapper.toDomain(repository.save(GameMappingMapper.toEntity(mapping)));
    }
}
