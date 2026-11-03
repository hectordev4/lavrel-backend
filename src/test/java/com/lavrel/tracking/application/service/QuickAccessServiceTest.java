package com.lavrel.tracking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.tracking.application.port.out.TrackedGameCardQueryPort;
import com.lavrel.tracking.application.port.out.TrackedGameRepositoryPort;
import com.lavrel.tracking.domain.GameNotTrackedException;
import com.lavrel.tracking.domain.TrackedGame;
import com.lavrel.tracking.domain.TrackingStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QuickAccessServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");

    private final TrackedGameRepositoryPort repo = mock(TrackedGameRepositoryPort.class);
    private final TrackedGameCardQueryPort cards = mock(TrackedGameCardQueryPort.class);
    private final QuickAccessService service = new QuickAccessService(repo, cards, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void pinningAnUntrackedGameIs404() {
        when(repo.find(1L, 7L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.pin(1L, 7L)).isInstanceOf(GameNotTrackedException.class);
    }

    @Test
    void pinningAddsAtTheEnd() {
        TrackedGame entry = TrackedGame.track(1L, 7L, TrackingStatus.PLAYING, NOW);
        when(repo.find(1L, 7L)).thenReturn(Optional.of(entry));
        when(repo.findPinned(1L)).thenReturn(List.of(TrackedGame.restore(1L, 3L, TrackingStatus.PLAYING, 1, NOW, NOW)));

        service.pin(1L, 7L);

        assertThat(entry.pinPosition()).isEqualTo(2);
        verify(repo).save(entry);
    }

    @Test
    void pinningTwiceChangesNothing() {
        when(repo.find(1L, 7L)).thenReturn(Optional.of(TrackedGame.restore(1L, 7L, TrackingStatus.PLAYING, 1, NOW, NOW)));

        service.pin(1L, 7L);

        verify(repo, never()).save(any());
    }

    @Test
    void unpinningClosesTheGap() {
        TrackedGame first = TrackedGame.restore(1L, 7L, TrackingStatus.PLAYING, 1, NOW, NOW);
        TrackedGame second = TrackedGame.restore(1L, 8L, TrackingStatus.PLAYING, 2, NOW, NOW);
        when(repo.find(1L, 7L)).thenReturn(Optional.of(first));
        when(repo.findPinned(1L)).thenReturn(List.of(second));

        service.unpin(1L, 7L);

        assertThat(first.pinned()).isFalse();
        assertThat(second.pinPosition()).isEqualTo(1);
        verify(repo).saveAll(List.of(second));
    }
}
