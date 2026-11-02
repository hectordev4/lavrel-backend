package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.out.PlatformAccountRepositoryPort;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.PlatformAccountNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class GetPlatformAccountServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    private final PlatformAccountRepositoryPort accounts = mock(PlatformAccountRepositoryPort.class);
    private final GetPlatformAccountService service = new GetPlatformAccountService(accounts);

    @Test
    void ownAccountIsFoundByPlatformAndExternalId() {
        when(accounts.findByPlatformAndExternalId(Platform.STEAM, "7656"))
                .thenReturn(Optional.of(PlatformAccount.restore(5L, 1L, Platform.STEAM, "7656", "Nick", true, NOW,
                        NOW, null)));

        assertThat(service.getOwned(1L, Platform.STEAM, "7656").id()).isEqualTo(5L);
    }

    @Test
    void someoneElsesAccountLooksLikeAMissingOne() {
        when(accounts.findByPlatformAndExternalId(Platform.STEAM, "7656"))
                .thenReturn(Optional.of(PlatformAccount.restore(9L, 2L, Platform.STEAM, "7656", null, true, NOW,
                        NOW, null)));

        assertThatThrownBy(() -> service.getOwned(1L, Platform.STEAM, "7656"))
                .isInstanceOf(PlatformAccountNotFoundException.class)
                .hasMessageNotContaining("9");
    }

    @Test
    void unknownAccountIsNotFound() {
        when(accounts.findByPlatformAndExternalId(Platform.STEAM, "1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwned(1L, Platform.STEAM, "1"))
                .isInstanceOf(PlatformAccountNotFoundException.class);
    }
}
