package com.lavrel.user.application.port.out;

import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.User;
import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findById(long id);

    Optional<User> findByFirebaseUid(String firebaseUid);

    Optional<User> findByPublicId(long publicId);

    boolean existsByPublicId(long publicId);

    /** Inserts when id is null (a concurrent first login of the same Firebase user is absorbed), else updates. */
    User save(User user);

    long countByRole(Role role);

    /** Deletes the user and, through ON DELETE CASCADE, everything attached to it. */
    void delete(long id);
}
