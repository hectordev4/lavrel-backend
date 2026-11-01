package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.InvalidRoleChangeException;
import com.lavrel.user.domain.PublicIds;
import com.lavrel.user.domain.PublicUserNotFoundException;
import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.User;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChangeUserRoleServiceTest {

    private static final long TARGET_PUBLIC_ID = PublicIds.MIN + 42;

    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final ChangeUserRoleService service = new ChangeUserRoleService(users);

    private static User user(long id, Role role) {
        return new User(id, TARGET_PUBLIC_ID, "uid-" + id, "name", null, true, null, null, role, Instant.EPOCH);
    }

    @Test
    void adminPromotesSomeoneElse() {
        User target = user(2L, Role.USER);
        when(users.findByPublicId(TARGET_PUBLIC_ID)).thenReturn(Optional.of(target));

        service.changeRole(1L, TARGET_PUBLIC_ID, Role.SUPPORT);

        verify(users).save(target.withRole(Role.SUPPORT));
    }

    @Test
    void adminCannotChangeTheirOwnRole() {
        when(users.findByPublicId(TARGET_PUBLIC_ID)).thenReturn(Optional.of(user(1L, Role.ADMIN)));

        assertThatThrownBy(() -> service.changeRole(1L, TARGET_PUBLIC_ID, Role.USER))
                .isInstanceOf(InvalidRoleChangeException.class);
        verify(users, never()).save(any());
    }

    @Test
    void unknownPublicIdIsNotFound() {
        when(users.findByPublicId(TARGET_PUBLIC_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.changeRole(1L, TARGET_PUBLIC_ID, Role.ADMIN))
                .isInstanceOf(PublicUserNotFoundException.class);
    }
}
