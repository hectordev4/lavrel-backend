package catalog.adapter.out.persistence;

import catalog.adapter.out.persistence.mapper.GameMapper;
import catalog.adapter.out.persistence.repository.GameJpaRepository;
import catalog.application.port.out.GameRepositoryPort;
import catalog.domain.Game;
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
}
