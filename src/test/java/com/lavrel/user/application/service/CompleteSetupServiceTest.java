package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.PublicIds;
import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.User;
import com.lavrel.user.domain.UserNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CompleteSetupServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");
    private static final Instant FIRST_TIME = Instant.parse("2026-11-10T08:00:00Z");

    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final CompleteSetupService service =
            new CompleteSetupService(users, Clock.fixed(NOW, ZoneOffset.UTC));

    private static User stored(Instant setupCompletedAt) {
        return User.restore(7L, PublicIds.MIN + 7, "uid-7", "SwiftOtter", "a@b.c", true, null, null, Role.USER,
                Instant.EPOCH, setupCompletedAt);
    }

    @Test
    void theFirstCallRecordsTheTime() {
        when(users.findById(7L)).thenReturn(Optional.of(stored(null)));

        service.completeSetup(7L);

        verify(users).markSetupCompleted(7L, NOW);
    }

    @Test
    void aSecondCallKeepsTheFirstTime() {
        when(users.findById(7L)).thenReturn(Optional.of(stored(FIRST_TIME)));

        service.completeSetup(7L);

        verify(users, never()).markSetupCompleted(anyLong(), any());
    }

    @Test
    void unknownUser() {
        when(users.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeSetup(7L)).isInstanceOf(UserNotFoundException.class);
    }
}
