package user.adapter.out.persistence;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import user.adapter.out.persistence.mapper.UserMapper;
import user.adapter.out.persistence.repository.UserJpaRepository;
import user.application.port.out.UserRepositoryPort;
import user.domain.User;

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

    /**
     * New users go through INSERT ... ON CONFLICT DO NOTHING and are then re-read, so two
     * simultaneous first logins of the same Firebase user end up with one row and no exception
     * (a failed insert would also poison the surrounding Postgres transaction).
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
        return UserMapper.toDomain(repository.save(UserMapper.toEntity(user)));
    }
}
