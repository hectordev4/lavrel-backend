package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.GetCurrentUserUseCase;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.User;
import com.lavrel.user.domain.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetCurrentUserService implements GetCurrentUserUseCase {

    private final UserRepositoryPort users;

    public GetCurrentUserService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    public CurrentUserView getCurrentUser(long userId) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        return new CurrentUserView(user.id(), user.firebaseUid(), user.username(), user.email(),
                user.photoUrl(), user.countryId());
    }
}
