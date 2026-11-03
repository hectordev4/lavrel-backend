package com.lavrel.library.application.port.out;

import java.time.Instant;
import java.util.List;
import com.lavrel.library.domain.ProfilePrivateException;
import com.lavrel.shared.domain.PlatformBound;

/** Implemented by the platform clients. Returns raw platform data; translation happens in the service. */
public interface PlatformLibraryPort extends PlatformBound {

    /** @throws ProfilePrivateException if the profile is private */
    List<PlatformGame> fetchOwnedGames(String externalId);

    List<PlatformUnlock> fetchUnlocks(String externalId, String platformGameId);

    record PlatformGame(String platformGameId, String title, int playtimeMinutes, Instant lastPlayedAt) {
    }

    record PlatformUnlock(String platformAchievementId, Instant unlockedAt) {
    }
}
