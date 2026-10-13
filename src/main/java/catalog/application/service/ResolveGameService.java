package catalog.application.service;

import catalog.application.port.in.ResolveGameUseCase;
import catalog.application.port.out.GameMappingRepositoryPort;
import catalog.domain.GameMapping;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shared.domain.Platform;

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
