package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.PublicIds;
import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.User;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetCurrentUserServiceTest {

    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final GetCurrentUserService service = new GetCurrentUserService(users);

    private static User stored(Instant setupCompletedAt) {
        return User.restore(7L, PublicIds.MIN + 7, "uid-7", "SwiftOtter", "a@b.c", true, null, null, Role.USER,
                Instant.EPOCH, setupCompletedAt);
    }

    @Test
    void aBrandNewUserHasNotCompletedTheSetup() {
        when(users.findById(7L)).thenReturn(Optional.of(stored(null)));

        assertThat(service.getCurrentUser(7L).setupCompleted()).isFalse();
    }

    @Test
    void aUserWhoSavedOrSkippedTheScreenHasCompletedIt() {
        when(users.findById(7L)).thenReturn(Optional.of(stored(Instant.parse("2026-11-14T10:00:00Z"))));

        assertThat(service.getCurrentUser(7L).setupCompleted()).isTrue();
    }
}
