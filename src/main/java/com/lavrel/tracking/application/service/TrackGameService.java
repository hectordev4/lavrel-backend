package com.lavrel.tracking.application.service;

import com.lavrel.catalog.application.port.in.CheckPublishedGameUseCase;
import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.TrackedGameCard;
import com.lavrel.tracking.application.port.in.TrackGameUseCase;
import com.lavrel.tracking.application.port.out.TrackedGameCardQueryPort;
import com.lavrel.tracking.application.port.out.TrackedGameRepositoryPort;
import com.lavrel.tracking.domain.GameNotTrackedException;
import com.lavrel.tracking.domain.QuickAccess;
import com.lavrel.tracking.domain.TrackedGame;
import com.lavrel.tracking.domain.TrackingStatus;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackGameService implements TrackGameUseCase {

    private final CheckPublishedGameUseCase catalog;
    private final TrackedGameRepositoryPort trackedGames;
    private final TrackedGameCardQueryPort cards;
    private final Clock clock;

    public TrackGameService(CheckPublishedGameUseCase catalog, TrackedGameRepositoryPort trackedGames,
                            TrackedGameCardQueryPort cards, Clock clock) {
        this.catalog = catalog;
        this.trackedGames = trackedGames;
        this.cards = cards;
        this.clock = clock;
    }

    @Override
    @Transactional
    public TrackedGameCard track(long userId, long gameId, TrackingStatus status) {
        catalog.requirePublished(gameId);
        Instant now = clock.instant();
        TrackedGame entry = trackedGames.find(userId, gameId).orElse(null);
        if (entry == null) {
            trackedGames.save(TrackedGame.track(userId, gameId, status, now));
        } else if (entry.changeStatus(status, now)) {
            trackedGames.save(entry);
        }
        return cards.findCard(userId, gameId).stream().findFirst()
                .orElseThrow(() -> new GameNotTrackedException(gameId));
    }

    @Override
    @Transactional
    public void untrack(long userId, long gameId) {
        TrackedGame entry = trackedGames.find(userId, gameId).orElseThrow(() -> new GameNotTrackedException(gameId));
        trackedGames.delete(userId, gameId);
        if (entry.pinned()) {
            trackedGames.saveAll(QuickAccess.closeGaps(trackedGames.findPinned(userId), clock.instant()));
        }
    }
}
