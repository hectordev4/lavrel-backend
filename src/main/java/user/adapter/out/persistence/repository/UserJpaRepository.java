package com.yourapp.user.adapter.out.persistence.repository;

import com.yourapp.user.adapter.out.persistence.entity.UserJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserJpaRepository extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByFirebaseUid(String firebaseUid);
}
