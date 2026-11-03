package com.lavrel.user.application.port.in;

import com.lavrel.user.domain.UserNotFoundException;

public interface GetUserCountryUseCase {

    /**
     * @return the country id, or null if the user has not set one
     * @throws UserNotFoundException if the user does not exist
     */
    Integer getCountryId(long userId);
}
