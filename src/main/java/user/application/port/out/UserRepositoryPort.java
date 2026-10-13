package user.application.port.out;

import java.util.Optional;
import user.domain.User;

public interface UserRepositoryPort {

    Optional<User> findById(long id);

    Optional<User> findByFirebaseUid(String firebaseUid);

    User save(User user);
}
