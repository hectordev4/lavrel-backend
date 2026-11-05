package com.lavrel.library.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.library.application.port.in.AutoSyncUseCase.AutoSyncReport;
import com.lavrel.library.application.port.in.SyncAccountUseCase;
import com.lavrel.library.application.port.in.SyncAccountUseCase.SyncResult;
import com.lavrel.library.application.port.out.DueAccountsQueryPort;
import com.lavrel.library.domain.ProfilePrivateException;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformUnavailableException;
import com.lavrel.user.application.port.in.MarkAccountSyncedUseCase;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class AutoSyncServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-05T12:00:00Z");

    private final DueAccountsQueryPort due = mock(DueAccountsQueryPort.class);
    private final SyncAccountUseCase sync = mock(SyncAccountUseCase.class);
    private final MarkAccountSyncedUseCase marker = mock(MarkAccountSyncedUseCase.class);
    private final AutoSyncService service = new AutoSyncService(due, sync, marker, Clock.fixed(NOW, ZoneOffset.UTC),
            Duration.ofHours(6), 30, 20, 0);

    @Test
    void asksForAccountsWaitingLongerThanTheIntervalFromActiveUsers() {
        when(due.findDue(any(), any(), anyInt())).thenReturn(List.of());

        service.runOnce();

        verify(due).findDue(NOW.minus(Duration.ofHours(6)), NOW.minus(Duration.ofDays(30)), 20);
    }

    @Test
    void syncsOnlyChangedGamesAndAddsUpUnlocks() {
        when(due.findDue(any(), any(), anyInt())).thenReturn(List.of(1L, 2L));
        when(sync.syncChangedGames(1L)).thenReturn(new SyncResult(10, 0, 3, 0));
        when(sync.syncChangedGames(2L)).thenReturn(new SyncResult(5, 1, 2, 0));

        AutoSyncReport report = service.runOnce();

        assertThat(report.accountsSynced()).isEqualTo(2);
        assertThat(report.unlocksRecorded()).isEqualTo(5);
    }

    @Test
    void aPrivateProfileDoesNotBlockTheQueue() {
        when(due.findDue(any(), any(), anyInt())).thenReturn(List.of(1L, 2L));
        when(sync.syncChangedGames(1L)).thenThrow(new ProfilePrivateException("7656"));
        when(sync.syncChangedGames(2L)).thenReturn(new SyncResult(1, 0, 0, 0));

        AutoSyncReport report = service.runOnce();

        verify(marker).markSynced(1L);
        assertThat(report.accountsFailed()).isEqualTo(1);
        assertThat(report.accountsSynced()).isEqualTo(1);
    }

    @Test
    void platformTroubleStopsTheRound() {
        when(due.findDue(any(), any(), anyInt())).thenReturn(List.of(1L, 2L));
        when(sync.syncChangedGames(1L)).thenThrow(new PlatformUnavailableException(Platform.STEAM, "429"));

        AutoSyncReport report = service.runOnce();

        assertThat(report.stoppedEarly()).isTrue();
        verify(sync, never()).syncChangedGames(2L);
    }
}
