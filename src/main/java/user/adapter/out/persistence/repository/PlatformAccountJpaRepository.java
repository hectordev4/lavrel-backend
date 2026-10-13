package com.yourapp.user.adapter.out.persistence.repository;

import com.yourapp.user.adapter.out.persistence.entity.PlatformAccountJpaEntity;
import java.util.Optional;
import java.util.List;
import com.yourapp.shared.domain.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformAccountJpaRepository extends JpaRepository<PlatformAccountJpaEntity, Long> {

    List<PlatformAccountJpaEntity> findByUserId(long userId);

    Optional<PlatformAccountJpaEntity> findByPlatformAndExternalId(Platform platform, String externalId);
}
