package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.CompleteSetupUseCase;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.User;
import com.lavrel.user.domain.UserNotFoundException;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompleteSetupService implements CompleteSetupUseCase {

    private final UserRepositoryPort users;
    private final Clock clock;

    public CompleteSetupService(UserRepositoryPort users, Clock clock) {
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void completeSetup(long userId) {
        User user = users.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
        Instant now = clock.instant();
        if (user.completeSetup(now)) {
            // Only writes while it is still empty, so a call at the same moment cannot replace the first time.
            users.markSetupCompleted(userId, now);
        }
    }
}
