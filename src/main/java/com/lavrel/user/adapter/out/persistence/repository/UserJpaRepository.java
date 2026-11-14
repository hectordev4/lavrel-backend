package com.lavrel.user.adapter.out.persistence.repository;

import com.lavrel.user.adapter.out.persistence.entity.UserJpaEntity;
import com.lavrel.user.domain.Role;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByFirebaseUid(String firebaseUid);

    Optional<UserJpaEntity> findByPublicId(long publicId);

    boolean existsByPublicId(long publicId);

    long countByRole(Role role);

    /** Race-safe first login: a concurrent insert of the same firebase_uid is silently ignored. */
    @Modifying
    @Query(value = """
            INSERT INTO users (public_id, firebase_uid, username, email, email_verified, photo_url, country_id, role,
                               created_at)
            VALUES (:publicId, :firebaseUid, :username, :email, :emailVerified, :photoUrl, :countryId, :role,
                    :createdAt)
            ON CONFLICT (firebase_uid) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("publicId") long publicId,
                       @Param("firebaseUid") String firebaseUid,
                       @Param("username") String username,
                       @Param("email") String email,
                       @Param("emailVerified") boolean emailVerified,
                       @Param("photoUrl") String photoUrl,
                       @Param("countryId") Integer countryId,
                       @Param("role") String role,
                       @Param("createdAt") Instant createdAt);

    /**
     * Marks the first setup as done, only if it was not already: the first time is kept, and two calls at the same
     * moment cannot overwrite each other.
     */
    @Modifying
    @Query(value = "UPDATE users SET setup_completed_at = :at WHERE id = :id AND setup_completed_at IS NULL",
            nativeQuery = true)
    int markSetupCompleted(@Param("id") long id, @Param("at") Instant at);
}
