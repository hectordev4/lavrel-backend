package com.lavrel.tracking.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.catalog.application.port.in.CheckPublishedGameUseCase;
import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.TrackedGameCard;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TrackGameServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-03T10:00:00Z");

    private final CheckPublishedGameUseCase catalog = mock(CheckPublishedGameUseCase.class);
    private final TrackedGameRepositoryPort repo = mock(TrackedGameRepositoryPort.class);
    private final TrackedGameCardQueryPort cards = mock(TrackedGameCardQueryPort.class);
    private final TrackGameService service =
            new TrackGameService(catalog, repo, cards, Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        when(cards.findCard(1L, 7L)).thenReturn(List.of(new TrackedGameCard(7L, "Game", null, null,
                TrackingStatus.PLAYING, null, 0, 10, List.of(), NOW, NOW)));
    }

    @Test
    void newGameIsAddedWithItsStatus() {
        when(repo.find(1L, 7L)).thenReturn(Optional.empty());

        service.track(1L, 7L, TrackingStatus.PLAYING);

        ArgumentCaptor<TrackedGame> saved = ArgumentCaptor.forClass(TrackedGame.class);
        verify(repo).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(TrackingStatus.PLAYING);
    }

    @Test
    void sameStatusIsNotWrittenAgain() {
        when(repo.find(1L, 7L)).thenReturn(Optional.of(TrackedGame.track(1L, 7L, TrackingStatus.PLAYING, NOW)));

        service.track(1L, 7L, TrackingStatus.PLAYING);

        verify(repo, never()).save(any());
    }

    @Test
    void gamesOutsideTheCatalogCannotBeTracked() {
        doThrow(new GameNotFoundException(99L)).when(catalog).requirePublished(99L);

        assertThatThrownBy(() -> service.track(1L, 99L, TrackingStatus.BACKLOG))
                .isInstanceOf(GameNotFoundException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void untrackingAGameThatIsNotTrackedIs404() {
        when(repo.delete(1L, 7L)).thenReturn(false);

        assertThatThrownBy(() -> service.untrack(1L, 7L)).isInstanceOf(GameNotTrackedException.class);
    }
}
