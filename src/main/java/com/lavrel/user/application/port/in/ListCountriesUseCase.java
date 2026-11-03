package com.lavrel.user.application.port.in;

import java.util.List;
import com.lavrel.user.domain.Country;

public interface ListCountriesUseCase {

    List<Country> listCountries();
}
