package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.ResolveAchievementUseCase;
import com.lavrel.catalog.application.port.out.AchievementMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.AchievementRepositoryPort;
import com.lavrel.catalog.domain.Achievement;
import com.lavrel.catalog.domain.AchievementMapping;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.shared.domain.Platform;

@Service
@Transactional(readOnly = true)
public class ResolveAchievementService implements ResolveAchievementUseCase {

    private final AchievementRepositoryPort achievements;
    private final AchievementMappingRepositoryPort mappings;

    public ResolveAchievementService(AchievementRepositoryPort achievements,
                                     AchievementMappingRepositoryPort mappings) {
        this.achievements = achievements;
        this.mappings = mappings;
    }

    @Override
    public Map<String, Long> resolveAchievementIds(long gameId, Platform platform,
                                                   Collection<String> platformAchievementIds) {
        if (platformAchievementIds.isEmpty()) {
            return Map.of();
        }
        if (platform == Platform.STEAM) {
            // Steam is the canonical source: its achievement IDs are our steamAchievementId.
            Set<String> wanted = new HashSet<>(platformAchievementIds);
            return achievements.findByGameId(gameId).stream()
                    .filter(a -> wanted.contains(a.steamAchievementId()))
                    .collect(Collectors.toMap(Achievement::steamAchievementId, a -> a.id()));
        }
        return mappings.findByGameAndPlatform(gameId, platform, platformAchievementIds).stream()
                .filter(AchievementMapping::available)
                .collect(Collectors.toMap(AchievementMapping::platformAchievementId,
                        AchievementMapping::achievementId, (a, b) -> a));
    }
}
