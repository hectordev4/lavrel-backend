package com.lavrel.user.adapter.out.persistence;

import com.lavrel.user.adapter.out.persistence.mapper.UserMapper;
import com.lavrel.user.adapter.out.persistence.repository.UserJpaRepository;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.User;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class UserPersistenceAdapter implements UserRepositoryPort {

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
    public Optional<User> findByPublicId(long publicId) {
        return repository.findByPublicId(publicId).map(UserMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPublicId(long publicId) {
        return repository.existsByPublicId(publicId);
    }

    /**
     * New users go through INSERT ... ON CONFLICT DO NOTHING and are then re-read, so two
     * simultaneous first logins of the same Firebase user end up with one row and no exception
     * (a failed insert would also poison the surrounding Postgres transaction).
     */
    @Override
    @Transactional
    public User save(User user) {
        if (user.id() == null) {
            repository.insertIfAbsent(user.publicId(), user.firebaseUid(), user.username(), user.email(),
                    user.emailVerified(), user.photoUrl(), user.countryId(), user.role().name(), user.createdAt());
            return repository.findByFirebaseUid(user.firebaseUid())
                    .map(UserMapper::toDomain)
                    .orElseThrow(() -> new IllegalStateException("User vanished after insert: " + user.firebaseUid()));
        }
        return UserMapper.toDomain(repository.saveAndFlush(UserMapper.toEntity(user)));
    }
}
