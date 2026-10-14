package user.adapter.out.persistence.repository;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import user.adapter.out.persistence.entity.UserJpaEntity;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByFirebaseUid(String firebaseUid);

    /** Race-safe first login: a concurrent insert of the same firebase_uid is silently ignored. */
    @Modifying
    @Query(value = """
            INSERT INTO users (firebase_uid, username, email, photo_url, country_id, created_at)
            VALUES (:firebaseUid, :username, :email, :photoUrl, :countryId, :createdAt)
            ON CONFLICT (firebase_uid) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("firebaseUid") String firebaseUid,
                       @Param("username") String username,
                       @Param("email") String email,
                       @Param("photoUrl") String photoUrl,
                       @Param("countryId") Integer countryId,
                       @Param("createdAt") Instant createdAt);
}
