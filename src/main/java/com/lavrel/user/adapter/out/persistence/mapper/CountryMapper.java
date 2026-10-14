package com.lavrel.user.adapter.out.persistence.mapper;

import com.lavrel.user.adapter.out.persistence.entity.CountryJpaEntity;
import com.lavrel.user.domain.Country;

public final class CountryMapper {

    private CountryMapper() {
    }

    public static Country toDomain(CountryJpaEntity e) {
        return new Country(e.getId(), e.getCode(), e.getName());
    }

    public static CountryJpaEntity toEntity(Country d) {
        return new CountryJpaEntity(d.id(), d.code(), d.name());
    }
}
