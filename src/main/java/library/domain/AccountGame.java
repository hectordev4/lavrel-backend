package library.domain;

import java.time.Instant;

/** PLATFORM. Synced per platform account. Key: (platformAccountId, gameId). */
public record AccountGame(
        long platformAccountId,
        long gameId,
        int playtimeMinutes,
        Instant lastPlayedAt,
        Instant lastSyncedAt) {

    public AccountGame {
        if (playtimeMinutes < 0) {
            throw new IllegalArgumentException("playtimeMinutes must be >= 0");
        }
    }
}