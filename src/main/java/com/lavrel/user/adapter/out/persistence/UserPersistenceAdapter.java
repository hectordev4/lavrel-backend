package com.lavrel.user.adapter.out.persistence;

import com.lavrel.user.adapter.out.persistence.mapper.UserMapper;
import com.lavrel.user.adapter.out.persistence.repository.UserJpaRepository;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.User;
import com.lavrel.user.domain.UsernameTakenException;
import java.util.Optional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

    /** Must match the unique index name in migration V11. */
    static final String UNIQUE_USERNAME = "uq_users_username_lower";

    private final UserJpaRepository repository;

    public UserPersistenceAdapter(UserJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findById(long id) {
        return repository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByFirebaseUid(String firebaseUid) {
        return repository.findByFirebaseUid(firebaseUid).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return repository.existsByUsernameIgnoreCase(username);
    }

    /**
     * New users go through INSERT ... ON CONFLICT DO NOTHING and are then re-read, so two
     * simultaneous first logins of the same Firebase user end up with one row and no exception
     * (a failed insert would also poison the surrounding Postgres transaction).
     * Updates are flushed immediately so a username that was taken a moment ago surfaces here.
     */
    @Override
    @Transactional
    public User save(User user) {
        if (user.id() == null) {
            repository.insertIfAbsent(user.firebaseUid(), user.username(), user.email(),
                    user.photoUrl(), user.countryId(), user.createdAt());
            return repository.findByFirebaseUid(user.firebaseUid())
                    .map(UserMapper::toDomain)
                    .orElseThrow(() -> new IllegalStateException("User vanished after insert: " + user.firebaseUid()));
        }
        try {
            return UserMapper.toDomain(repository.saveAndFlush(UserMapper.toEntity(user)));
        } catch (DataIntegrityViolationException e) {
            if (violatesUniqueUsername(e)) {
                throw new UsernameTakenException(user.username());
            }
            throw e;
        }
    }

    private static boolean violatesUniqueUsername(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof ConstraintViolationException cve
                    && UNIQUE_USERNAME.equalsIgnoreCase(cve.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}
