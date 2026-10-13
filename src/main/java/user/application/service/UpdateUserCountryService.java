package user.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.application.port.in.UpdateUserCountryUseCase;
import user.application.port.out.CountryRepositoryPort;
import user.application.port.out.UserRepositoryPort;
import user.domain.UserNotFoundException;

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
            throw new IllegalArgumentException("Unknown country: " + countryId);
        }
        users.save(user.withCountry(countryId));
    }
}
