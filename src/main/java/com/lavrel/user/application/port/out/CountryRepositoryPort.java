package com.lavrel.user.application.port.out;

import java.util.List;
import java.util.Optional;
import com.lavrel.user.domain.Country;

public interface CountryRepositoryPort {

    Optional<Country> findById(int id);

    List<Country> findAll();

    boolean existsById(int id);
}
