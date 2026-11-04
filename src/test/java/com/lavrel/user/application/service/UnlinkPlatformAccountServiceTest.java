package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.progress.application.port.in.RecordUnlocksUseCase;
import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.out.PlatformAccountRepositoryPort;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.PlatformAccountNotFoundException;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class UnlinkPlatformAccountServiceTest {

    private static final Instant T = Instant.parse("2026-11-04T10:00:00Z");

    private final PlatformAccountRepositoryPort accounts = mock(PlatformAccountRepositoryPort.class);
    private final RecordUnlocksUseCase unlocks = mock(RecordUnlocksUseCase.class);
    private final UnlinkPlatformAccountService service = new UnlinkPlatformAccountService(accounts, unlocks);

    @Test
    void unlocksFromTheAccountGoFirstThenTheLink() {
        when(accounts.findByPlatformAndExternalId(Platform.STEAM, "7656"))
                .thenReturn(Optional.of(PlatformAccount.restore(5L, 1L, Platform.STEAM, "7656", null, true, T, T, T)));

        service.unlink(1L, Platform.STEAM, "7656");

        InOrder order = inOrder(unlocks, accounts);
        order.verify(unlocks).forgetAccount(1L, 5L);
        order.verify(accounts).delete(5L);
    }

    @Test
    void someoneElsesAccountCannotBeUnlinked() {
        when(accounts.findByPlatformAndExternalId(Platform.STEAM, "7656"))
                .thenReturn(Optional.of(PlatformAccount.restore(5L, 2L, Platform.STEAM, "7656", null, true, T, T, T)));

        assertThatThrownBy(() -> service.unlink(1L, Platform.STEAM, "7656"))
                .isInstanceOf(PlatformAccountNotFoundException.class);
        verify(accounts, never()).delete(anyLong());
    }
}
