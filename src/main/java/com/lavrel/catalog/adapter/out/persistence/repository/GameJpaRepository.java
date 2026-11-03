package com.lavrel.catalog.adapter.out.persistence.repository;

import com.lavrel.catalog.adapter.out.persistence.entity.GameJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameJpaRepository extends JpaRepository<GameJpaEntity, Long> {
}
