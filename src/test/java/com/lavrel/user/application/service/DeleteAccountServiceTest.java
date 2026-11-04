package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.user.application.port.out.IdentityProviderAdminPort;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.LastAdminException;
import com.lavrel.user.domain.PublicIds;
import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.User;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class DeleteAccountServiceTest {

    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private final IdentityProviderAdminPort firebase = mock(IdentityProviderAdminPort.class);
    private final DeleteAccountService service = new DeleteAccountService(users, firebase);

    private static User user(Role role) {
        return User.restore(7L, PublicIds.MIN + 7, "uid-7", "SwiftOtter", "a@b.c", true, null, null, role,
                Instant.EPOCH);
    }

    @Test
    void ourDataGoesFirstThenTheSignInAccount() {
        when(users.findById(7L)).thenReturn(Optional.of(user(Role.USER)));

        service.deleteAccount(7L);

        InOrder order = inOrder(users, firebase);
        order.verify(users).delete(7L);
        order.verify(firebase).deleteUser("uid-7");
    }

    @Test
    void theOnlyAdminCannotLeave() {
        when(users.findById(7L)).thenReturn(Optional.of(user(Role.ADMIN)));
        when(users.countByRole(Role.ADMIN)).thenReturn(1L);

        assertThatThrownBy(() -> service.deleteAccount(7L)).isInstanceOf(LastAdminException.class);
        verify(users, never()).delete(anyLong());
        verify(firebase, never()).deleteUser(anyString());
    }

    @Test
    void anAdminCanLeaveWhenThereAreOthers() {
        when(users.findById(7L)).thenReturn(Optional.of(user(Role.ADMIN)));
        when(users.countByRole(Role.ADMIN)).thenReturn(2L);

        service.deleteAccount(7L);

        verify(users).delete(7L);
    }
}
