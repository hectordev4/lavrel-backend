package user.application.port.in;

import java.util.List;
import user.domain.Country;

public interface ListCountriesUseCase {

    List<Country> listCountries();
}
