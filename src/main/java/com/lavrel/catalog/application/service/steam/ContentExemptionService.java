package com.lavrel.catalog.application.service.steam;

import com.lavrel.catalog.application.port.in.ManageContentExemptionsUseCase;
import com.lavrel.catalog.application.port.out.ContentExemptionRepositoryPort;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.ImportFailureRepositoryPort;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.shared.domain.Platform;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Every exception added or removed is logged, so there is a trace of which games bypass the policy. */
@Service
public class ContentExemptionService implements ManageContentExemptionsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ContentExemptionService.class);

    private final GameMappingRepositoryPort gameMappings;
    private final ImportFailureRepositoryPort failures;
    private final ContentExemptionRepositoryPort exemptions;
    private final SteamImportRun importRun;
    private final long delayMs;

    ContentExemptionService(GameMappingRepositoryPort gameMappings, ImportFailureRepositoryPort failures,
                            ContentExemptionRepositoryPort exemptions, SteamImportRun importRun,
                            @Value("${app.catalog.import-delay-ms:300}") long delayMs) {
        this.gameMappings = gameMappings;
        this.failures = failures;
        this.exemptions = exemptions;
        this.importRun = importRun;
        this.delayMs = delayMs;
    }

    @Override
    public List<ExemptionResult> exempt(List<String> steamAppIds) {
        List<String> ids = steamAppIds.stream().distinct().toList();
        Map<String, Long> known = new LinkedHashMap<>();
        for (GameMapping mapping : gameMappings.findByPlatformAndPlatformGameIds(Platform.STEAM, ids)) {
            known.put(mapping.platformGameId(), mapping.gameId());
        }
        List<String> unknown = ids.stream().filter(id -> !known.containsKey(id)).toList();

        unknown.forEach(id -> failures.clearFailure(Platform.STEAM, id));
        Map<String, Long> imported = unknown.isEmpty()
                ? Map.of()
                : importRun.run(unknown, unknown.size(), ImportMode.EXEMPT, delayMs).imported();

        List<ExemptionResult> results = new ArrayList<>();
        for (String id : ids) {
            if (known.containsKey(id)) {
                results.add(flag(id, known.get(id), Status.EXEMPTED));
            } else if (imported.containsKey(id)) {
                results.add(flag(id, imported.get(id), Status.IMPORTED));
            } else {
                results.add(failures.findReason(Platform.STEAM, id)
                        .map(reason -> new ExemptionResult(id, Status.NOT_IMPORTABLE, null, reason))
                        .orElseGet(() -> new ExemptionResult(id, Status.DEFERRED, null,
                                "Steam could not be reached; try again")));
            }
        }
        return results;
    }

    @Override
    public boolean removeExemption(String steamAppId) {
        List<GameMapping> mappings = gameMappings.findByPlatformAndPlatformGameIds(Platform.STEAM, List.of(steamAppId));
        if (mappings.isEmpty()) {
            return false;
        }
        exemptions.setExempt(mappings.get(0).gameId(), false);
        log.info("Content-policy exception removed for Steam app {}", steamAppId);
        return true;
    }

    @Override
    public List<ExemptGame> listExemptions() {
        return exemptions.findAll().stream()
                .map(g -> new ExemptGame(g.gameId(), g.title(), g.steamAppId()))
                .toList();
    }

    private ExemptionResult flag(String steamAppId, long gameId, Status status) {
        exemptions.setExempt(gameId, true);
        log.info("Content-policy exception added for Steam app {} (game {}, {})", steamAppId, gameId, status);
        return new ExemptionResult(steamAppId, status, gameId, null);
    }
}
