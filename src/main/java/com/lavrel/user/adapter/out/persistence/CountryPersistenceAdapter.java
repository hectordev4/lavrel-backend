package com.lavrel.user.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.user.adapter.out.persistence.mapper.CountryMapper;
import com.lavrel.user.adapter.out.persistence.repository.CountryJpaRepository;
import com.lavrel.user.application.port.out.CountryRepositoryPort;
import com.lavrel.user.domain.Country;

@Component
@Transactional(readOnly = true)
public class CountryPersistenceAdapter implements CountryRepositoryPort {

    private final CountryJpaRepository repository;

    public CountryPersistenceAdapter(CountryJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Country> findById(int id) {
        return repository.findById(id).map(CountryMapper::toDomain);
    }

    @Override
    public List<Country> findAll() {
        return repository.findAll(Sort.by("name")).stream().map(CountryMapper::toDomain).toList();
    }

    @Override
    public boolean existsById(int id) {
        return repository.existsById(id);
    }
}
