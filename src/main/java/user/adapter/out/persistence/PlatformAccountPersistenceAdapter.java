package user.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import user.adapter.out.persistence.mapper.PlatformAccountMapper;
import user.adapter.out.persistence.repository.PlatformAccountJpaRepository;
import user.application.port.out.PlatformAccountRepositoryPort;
import user.domain.AccountAlreadyLinkedException;
import user.domain.PlatformAccount;

@Component
public class PlatformAccountPersistenceAdapter implements PlatformAccountRepositoryPort {

    /** Must match the unique constraint name in the Flyway migration. */
    static final String UNIQUE_PLATFORM_EXTERNAL_ID = "uq_platform_account_platform_external_id";

    private final PlatformAccountJpaRepository repository;

    public PlatformAccountPersistenceAdapter(PlatformAccountJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PlatformAccount> findById(long id) {
        return repository.findById(id).map(PlatformAccountMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PlatformAccount> findByUserId(long userId) {
        return repository.findByUserId(userId).stream().map(PlatformAccountMapper::toDomain).toList();
    }

    /**
     * saveAndFlush forces the INSERT now, so the unique-constraint violation surfaces here
     * and not later at commit. No check-then-insert: the database is the guarantee.
     */
    @Override
    public PlatformAccount save(PlatformAccount account) {
        try {
            return PlatformAccountMapper.toDomain(
                    repository.saveAndFlush(PlatformAccountMapper.toEntity(account)));
        } catch (DataIntegrityViolationException e) {
            if (violatesUniqueAccount(e)) {
                throw new AccountAlreadyLinkedException(account.platform(), account.externalId());
            }
            throw e;
        }
    }

    private static boolean violatesUniqueAccount(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve
                    && UNIQUE_PLATFORM_EXTERNAL_ID.equalsIgnoreCase(cve.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
