package com.lavrel.catalog.adapter.out.persistence;

import com.lavrel.catalog.adapter.out.persistence.mapper.GameMapper;
import com.lavrel.catalog.adapter.out.persistence.repository.GameJpaRepository;
import com.lavrel.catalog.application.port.out.GameRepositoryPort;
import com.lavrel.catalog.domain.Game;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class GamePersistenceAdapter implements GameRepositoryPort {

    private final GameJpaRepository repository;

    public GamePersistenceAdapter(GameJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Game> findById(long id) {
        return repository.findById(id).map(GameMapper::toDomain);
    }

    @Override
    @Transactional
    public Game save(Game game) {
        return GameMapper.toDomain(repository.save(GameMapper.toEntity(game)));
    }

    /** The foreign keys are ON DELETE CASCADE, so mappings, achievements, owners, unlocks and details go too. */
    @Override
    @Transactional
    public void delete(long id) {
        repository.deleteById(id);
    }
}
