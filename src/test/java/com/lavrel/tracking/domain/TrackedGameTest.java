package com.lavrel.tracking.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TrackedGameTest {

    private static final Instant T0 = Instant.parse("2026-11-03T10:00:00Z");
    private static final Instant T1 = T0.plusSeconds(60);

    @Test
    void newEntryIsNotPinned() {
        TrackedGame t = TrackedGame.track(1L, 7L, TrackingStatus.BACKLOG, T0);
        assertThat(t.pinned()).isFalse();
        assertThat(t.addedAt()).isEqualTo(T0).isEqualTo(t.updatedAt());
    }

    @Test
    void changeStatusReportsAndStampsRealChanges() {
        TrackedGame t = TrackedGame.track(1L, 7L, TrackingStatus.BACKLOG, T0);
        assertThat(t.changeStatus(TrackingStatus.BACKLOG, T1)).isFalse();
        assertThat(t.updatedAt()).isEqualTo(T0);
        assertThat(t.changeStatus(TrackingStatus.PLAYING, T1)).isTrue();
        assertThat(t.status()).isEqualTo(TrackingStatus.PLAYING);
        assertThat(t.updatedAt()).isEqualTo(T1);
    }

    @Test
    void pinAndUnpin() {
        TrackedGame t = TrackedGame.track(1L, 7L, TrackingStatus.PLAYING, T0);
        t.pinAt(3, T1);
        assertThat(t.pinPosition()).isEqualTo(3);
        t.unpin(T1);
        assertThat(t.pinned()).isFalse();
        assertThatThrownBy(() -> t.pinAt(0, T1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void identityIsUserAndGame() {
        assertThat(TrackedGame.track(1L, 7L, TrackingStatus.PLAYING, T0))
                .isEqualTo(TrackedGame.restore(1L, 7L, TrackingStatus.DROPPED, 2, T0, T1))
                .isNotEqualTo(TrackedGame.track(1L, 8L, TrackingStatus.PLAYING, T0));
    }
}
