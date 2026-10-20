package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.UpdateUsernameUseCase;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.User;
import com.lavrel.user.domain.UserNotFoundException;
import com.lavrel.user.domain.UsernameTakenException;
import com.lavrel.user.domain.Usernames;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateUsernameService implements UpdateUsernameUseCase {

    private final UserRepositoryPort users;

    public UpdateUsernameService(UserRepositoryPort users) {
        this.users = users;
    }

    @Override
    @Transactional
    public void updateUsername(long userId, String requested) {
        String username = Usernames.validate(requested);
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        boolean sameNameDifferentCase = user.username().equalsIgnoreCase(username);
        if (!sameNameDifferentCase && users.existsByUsername(username)) {
            throw new UsernameTakenException(username);
        }
        users.save(user.withProfile(username, user.email(), user.photoUrl()));
    }
}
