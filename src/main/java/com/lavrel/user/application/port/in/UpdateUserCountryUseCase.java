package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.CountryNotFoundException;
import com.lavrel.user.domain.UserNotFoundException;

public interface UpdateUserCountryUseCase {

    /**
     * Sets the user's country; null clears it (the user is then left out of country stats).
     *
     * @throws UserNotFoundException    if the user does not exist
     * @throws CountryNotFoundException if countryId is not null and does not exist
     */
    void updateCountry(long userId, Integer countryId);
}
