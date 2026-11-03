package com.lavrel.tracking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuickAccessTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");

    private static TrackedGame pinned(long gameId, int position) {
        return TrackedGame.restore(1L, gameId, TrackingStatus.PLAYING, position, NOW, NOW);
    }

    @Test
    void pinGoesToTheEnd() {
        TrackedGame entry = TrackedGame.track(1L, 9L, TrackingStatus.PLAYING, NOW);
        QuickAccess.pinAtEnd(entry, List.of(pinned(1, 1), pinned(2, 2)), NOW);
        assertThat(entry.pinPosition()).isEqualTo(3);
    }

    @Test
    void twentyIsTheLimit() {
        List<TrackedGame> full = new ArrayList<>();
        for (int i = 1; i <= QuickAccess.MAX_PINNED; i++) {
            full.add(pinned(i, i));
        }
        TrackedGame entry = TrackedGame.track(1L, 99L, TrackingStatus.PLAYING, NOW);
        assertThatThrownBy(() -> QuickAccess.pinAtEnd(entry, full, NOW)).isInstanceOf(QuickAccessFullException.class);
        assertThat(entry.pinned()).isFalse();
    }

    @Test
    void gapsAreClosed() {
        TrackedGame a = pinned(1, 1);
        TrackedGame c = pinned(3, 3);
        assertThat(QuickAccess.closeGaps(List.of(a, c), NOW)).containsExactly(c);
        assertThat(c.pinPosition()).isEqualTo(2);
    }

    @Test
    void reorderFollowsTheGivenOrder() {
        TrackedGame a = pinned(1, 1);
        TrackedGame b = pinned(2, 2);
        QuickAccess.reorder(List.of(a, b), List.of(2L, 1L), NOW);
        assertThat(b.pinPosition()).isEqualTo(1);
        assertThat(a.pinPosition()).isEqualTo(2);
    }

    @Test
    void reorderMustListExactlyThePinnedGames() {
        List<TrackedGame> pins = List.of(pinned(1, 1), pinned(2, 2));
        assertThatThrownBy(() -> QuickAccess.reorder(pins, List.of(1L), NOW))
                .isInstanceOf(InvalidQuickAccessOrderException.class);
        assertThatThrownBy(() -> QuickAccess.reorder(pins, List.of(1L, 1L), NOW))
                .isInstanceOf(InvalidQuickAccessOrderException.class);
        assertThatThrownBy(() -> QuickAccess.reorder(pins, List.of(1L, 3L), NOW))
                .isInstanceOf(InvalidQuickAccessOrderException.class);
    }
}
