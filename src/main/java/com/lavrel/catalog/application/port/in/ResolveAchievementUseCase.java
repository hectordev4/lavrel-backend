package com.lavrel.catalog.application.port.in;

import java.util.Collection;
import java.util.Map;
import com.lavrel.shared.domain.Platform;

public interface ResolveAchievementUseCase {

    /**
     * Resolves platform achievement IDs to our (Steam-based) achievement IDs.
     * The game is required because some platforms reuse achievement IDs across games.
     * Unmatched or unavailable achievements are absent from the result.
     *
     * @return platformAchievementId -> achievementId
     */
    Map<String, Long> resolveAchievementIds(long gameId, Platform platform,
                                            Collection<String> platformAchievementIds);
}
