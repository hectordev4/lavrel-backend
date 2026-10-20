package com.lavrel.user.application.port.out;

import com.lavrel.user.domain.User;
import com.lavrel.user.domain.UsernameTakenException;
import java.util.Optional;

public interface UserRepositoryPort {

    Optional<User> findById(long id);

    Optional<User> findByFirebaseUid(String firebaseUid);

    /** Case-insensitive. */
    boolean existsByUsername(String username);

    /** @throws UsernameTakenException if the username belongs to another user (the database enforces it) */
    User save(User user);
}
