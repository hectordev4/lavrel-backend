package com.lavrel.progress.application.port.in;

import java.time.Instant;
import java.util.List;

public interface RecordUnlocksUseCase {

    /**
     * Stores the unlocks, ignoring ones the user already has (one row per user and achievement).
     *
     * @return number of newly recorded unlocks
     */
    int record(Command command);

    record Command(long userId, Long sourceAccountId, List<Unlock> unlocks) {
    }

    record Unlock(long achievementId, Instant unlockedAt) {
    }
}
