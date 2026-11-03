package com.lavrel.catalog.application.port.in;

import java.util.Collection;
import java.util.Map;
import com.lavrel.shared.domain.Platform;

public interface ResolveGameUseCase {

    /**
     * Resolves platform game IDs to canonical game IDs through GameMapping.
     * Unmapped platform IDs are absent from the result (the caller skips or queues them).
     *
     * @return platformGameId -> gameId
     */
    Map<String, Long> resolveGameIds(Platform platform, Collection<String> platformGameIds);
}
