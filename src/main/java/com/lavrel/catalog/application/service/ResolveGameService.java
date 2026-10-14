package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.ResolveGameUseCase;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.domain.GameMapping;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.shared.domain.Platform;

@Service
@Transactional(readOnly = true)
public class ResolveGameService implements ResolveGameUseCase {

    private final GameMappingRepositoryPort mappings;

    public ResolveGameService(GameMappingRepositoryPort mappings) {
        this.mappings = mappings;
    }

    @Override
    public Map<String, Long> resolveGameIds(Platform platform, Collection<String> platformGameIds) {
        if (platformGameIds.isEmpty()) {
            return Map.of();
        }
        return mappings.findByPlatformAndPlatformGameIds(platform, platformGameIds).stream()
                .collect(Collectors.toMap(GameMapping::platformGameId, GameMapping::gameId, (a, b) -> a));
    }
}
