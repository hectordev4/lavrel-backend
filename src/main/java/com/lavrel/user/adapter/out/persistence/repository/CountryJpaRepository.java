package com.lavrel.user.adapter.out.persistence.repository;

import com.lavrel.user.adapter.out.persistence.entity.CountryJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryJpaRepository extends JpaRepository<CountryJpaEntity, Integer> {

    Optional<CountryJpaEntity> findByCode(String code);
}
