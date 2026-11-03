package com.lavrel.tracking.application.service;

import com.lavrel.tracking.application.port.in.GetTrackedGamesUseCase;
import com.lavrel.tracking.application.port.out.TrackedGameCardQueryPort;
import com.lavrel.tracking.domain.TrackingStatus;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class GetTrackedGamesService implements GetTrackedGamesUseCase {

    private final TrackedGameCardQueryPort cards;

    public GetTrackedGamesService(TrackedGameCardQueryPort cards) {
        this.cards = cards;
    }

    @Override
    public List<TrackedGameCard> list(long userId, TrackingStatus status, Sort sort) {
        return cards.findCards(userId, status, sort == null ? Sort.RELEASE : sort);
    }

    @Override
    public List<TrackedGameCard> quickAccess(long userId) {
        return cards.findPinnedCards(userId);
    }
}
