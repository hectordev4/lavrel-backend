package com.lavrel.user.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.in.LinkPlatformAccountUseCase.Command;
import com.lavrel.user.application.port.out.LinkStateSignerPort;
import com.lavrel.user.application.port.out.PlatformAccountRepositoryPort;
import com.lavrel.user.application.port.out.PlatformIdentityPort;
import com.lavrel.user.application.port.out.PlatformIdentityPort.PlatformIdentity;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.AccountAlreadyLinkedException;
import com.lavrel.user.domain.AccountLinkedElsewhereException;
import com.lavrel.user.domain.PlatformAccount;
import com.lavrel.user.domain.PublicIds;
import com.lavrel.user.domain.Role;
import com.lavrel.user.domain.User;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LinkPlatformAccountServiceTest {

    private static final Instant T = Instant.parse("2026-11-13T10:00:00Z");
    private static final Command COMMAND = new Command(Platform.STEAM, "state", Map.of());

    private final PlatformIdentityPort steam = mock(PlatformIdentityPort.class);
    private final LinkStateSignerPort signer = mock(LinkStateSignerPort.class);
    private final PlatformAccountRepositoryPort accounts = mock(PlatformAccountRepositoryPort.class);
    private final UserRepositoryPort users = mock(UserRepositoryPort.class);
    private LinkPlatformAccountService service;

    private static PlatformAccount linkedTo(long userId) {
        return PlatformAccount.restore(5L, userId, Platform.STEAM, "7656", "Nick", true, T, T, null);
    }

    @BeforeEach
    void setUp() {
        when(steam.platform()).thenReturn(Platform.STEAM);
        service = new LinkPlatformAccountService(List.of(steam), signer, accounts, users,
                Clock.fixed(T, ZoneOffset.UTC));
        when(signer.verify("state", Platform.STEAM)).thenReturn(1L);
        when(users.findById(1L)).thenReturn(Optional.of(User.restore(1L, PublicIds.MIN, "uid", "Otter", null, true,
                null, null, Role.USER, T)));
        when(steam.verify(any())).thenReturn(new PlatformIdentity("7656", "Nick"));
    }

    @Test
    void linkingMyOwnAccountAgainIsASuccessThatChangesNothing() {
        when(accounts.findByPlatformAndExternalId(Platform.STEAM, "7656")).thenReturn(Optional.of(linkedTo(1L)));

        assertThat(service.link(COMMAND).id()).isEqualTo(5L);
        verify(accounts, never()).save(any());
    }

    @Test
    void anAccountOfAnotherUserIsLinkedElsewhere() {
        when(accounts.findByPlatformAndExternalId(Platform.STEAM, "7656")).thenReturn(Optional.of(linkedTo(2L)));

        assertThatThrownBy(() -> service.link(COMMAND))
                .isInstanceOf(AccountLinkedElsewhereException.class)
                .hasMessageNotContaining("2");
        verify(accounts, never()).save(any());
    }

    @Test
    void aNewAccountIsSaved() {
        when(accounts.findByPlatformAndExternalId(Platform.STEAM, "7656")).thenReturn(Optional.empty());
        when(accounts.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThat(service.link(COMMAND).belongsTo(1L)).isTrue();
    }

    @Test
    void aRaceIsSettledTheSameWay() {
        when(accounts.findByPlatformAndExternalId(Platform.STEAM, "7656"))
                .thenReturn(Optional.empty(), Optional.of(linkedTo(1L)));
        when(accounts.save(any())).thenThrow(new AccountAlreadyLinkedException(Platform.STEAM, "7656"));

        assertThat(service.link(COMMAND).id()).isEqualTo(5L);
    }
}
