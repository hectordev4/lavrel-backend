package com.lavrel.user.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.user.application.port.in.GetUserCountryUseCase;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.UserNotFoundException;

@Service
@Transactional(readOnly = true)
public class GetUserCountryService implements GetUserCountryUseCase {

    private final UserRepositoryPort users;

    public GetUserCountryService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public Integer getCountryId(long userId) {
        return users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId)).countryId();
    }
}
