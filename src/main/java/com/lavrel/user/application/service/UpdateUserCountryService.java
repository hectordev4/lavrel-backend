package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.UpdateUserCountryUseCase;
import com.lavrel.user.application.port.out.CountryRepositoryPort;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.CountryNotFoundException;
import com.lavrel.user.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUserCountryService implements UpdateUserCountryUseCase {

    private final UserRepositoryPort users;
    private final CountryRepositoryPort countries;

    public UpdateUserCountryService(UserRepositoryPort users, CountryRepositoryPort countries) {
        this.users = users;
        this.countries = countries;
    }

    @Override
    @Transactional
    public void updateCountry(long userId, Integer countryId) {
        var user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        if (countryId != null && !countries.existsById(countryId)) {
            throw new CountryNotFoundException(countryId);
        }
        users.save(user.withCountry(countryId));
    }
}
