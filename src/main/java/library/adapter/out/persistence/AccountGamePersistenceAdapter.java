package library.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import library.adapter.out.persistence.mapper.AccountGameMapper;
import library.adapter.out.persistence.repository.AccountGameJpaRepository;
import library.application.port.out.AccountGameRepositoryPort;
import library.domain.AccountGame;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class AccountGamePersistenceAdapter implements AccountGameRepositoryPort {

    private final AccountGameJpaRepository repository;

    public AccountGamePersistenceAdapter(AccountGameJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountGame> findByPlatformAccountId(long platformAccountId) {
        return repository.findByIdPlatformAccountId(platformAccountId).stream()
                .map(AccountGameMapper::toDomain).toList();
    }

    /** The composite key (platformAccountId, gameId) makes saveAll an upsert. */
    @Override
    @Transactional
    public void saveAll(Collection<AccountGame> accountGames) {
        repository.saveAll(accountGames.stream().map(AccountGameMapper::toEntity).toList());
    }
}
