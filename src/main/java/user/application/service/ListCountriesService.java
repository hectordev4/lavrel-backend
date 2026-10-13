package user.application.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import user.application.port.in.ListCountriesUseCase;
import user.application.port.out.CountryRepositoryPort;
import user.domain.Country;

@Service
@Transactional(readOnly = true)
public class ListCountriesService implements ListCountriesUseCase {

    private final CountryRepositoryPort countries;

    public ListCountriesService(CountryRepositoryPort countries) {
        this.countries = countries;
    }

    @Override
    public List<Country> listCountries() {
        return countries.findAll();
    }
}
