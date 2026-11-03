package com.lavrel.tracking.application.service;

import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase.TrackedGameCard;
import com.lavrel.tracking.application.port.in.ManageQuickAccessUseCase;
import com.lavrel.tracking.application.port.out.TrackedGameCardQueryPort;
import com.lavrel.tracking.application.port.out.TrackedGameRepositoryPort;
import com.lavrel.tracking.domain.GameNotTrackedException;
import com.lavrel.tracking.domain.QuickAccess;
import com.lavrel.tracking.domain.TrackedGame;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QuickAccessService implements ManageQuickAccessUseCase {

    private final TrackedGameRepositoryPort trackedGames;
    private final TrackedGameCardQueryPort cards;
    private final Clock clock;

    public QuickAccessService(TrackedGameRepositoryPort trackedGames, TrackedGameCardQueryPort cards, Clock clock) {
        this.trackedGames = trackedGames;
        this.cards = cards;
        this.clock = clock;
    }

    @Override
    @Transactional
    public List<TrackedGameCard> pin(long userId, long gameId) {
        TrackedGame entry = trackedGames.find(userId, gameId).orElseThrow(() -> new GameNotTrackedException(gameId));
        if (!entry.pinned()) {
            QuickAccess.pinAtEnd(entry, trackedGames.findPinned(userId), clock.instant());
            trackedGames.save(entry);
        }
        return cards.findPinnedCards(userId);
    }

    @Override
    @Transactional
    public void unpin(long userId, long gameId) {
        TrackedGame entry = trackedGames.find(userId, gameId).orElseThrow(() -> new GameNotTrackedException(gameId));
        if (!entry.pinned()) {
            return;
        }
        Instant now = clock.instant();
        entry.unpin(now);
        trackedGames.save(entry);
        trackedGames.saveAll(QuickAccess.closeGaps(trackedGames.findPinned(userId), now));
    }

    @Override
    @Transactional
    public List<TrackedGameCard> reorder(long userId, List<Long> gameIds) {
        trackedGames.saveAll(QuickAccess.reorder(trackedGames.findPinned(userId), gameIds, clock.instant()));
        return cards.findPinnedCards(userId);
    }
}
