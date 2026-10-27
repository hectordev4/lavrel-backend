package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.CompareAchievementsUseCase;
import com.lavrel.catalog.application.port.out.AchievementMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.AchievementRepositoryPort;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.domain.Achievement;
import com.lavrel.catalog.domain.AchievementMapping;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.catalog.domain.GameNotOnPlatformException;
import com.lavrel.catalog.domain.InvalidComparisonException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformRegistry;

/**
 * Matches another platform's achievements against our Steam-based list.
 * Strategy: normalized name + description first, then normalized name alone when it is unique.
 * Anything left unmatched is stored as available = false.
 */
@Service
public class CompareAchievementsService implements CompareAchievementsUseCase {

    private final PlatformRegistry<PlatformCatalogPort> catalogPorts;
    private final GameMappingRepositoryPort gameMappings;
    private final AchievementRepositoryPort achievements;
    private final AchievementMappingRepositoryPort achievementMappings;
    private final TransactionTemplate tx;

    public CompareAchievementsService(List<PlatformCatalogPort> ports, GameMappingRepositoryPort gameMappings,
                                      AchievementRepositoryPort achievements,
                                      AchievementMappingRepositoryPort achievementMappings,
                                      TransactionTemplate tx) {
        this.catalogPorts = new PlatformRegistry<>(ports, "achievement comparison");
        this.gameMappings = gameMappings;
        this.achievements = achievements;
        this.achievementMappings = achievementMappings;
        this.tx = tx;
    }

    @Override
    public int compare(long gameId, Platform platform) {
        if (platform == Platform.STEAM) {
            throw new InvalidComparisonException("Steam is the base list; nothing to compare");
        }
        PlatformCatalogPort port = catalogPorts.require(platform);
        GameMapping gameMapping = gameMappings.findByGameIdAndPlatform(gameId, platform)
                .orElseThrow(() -> new GameNotOnPlatformException(gameId, platform));

        List<Achievement> steamList = achievements.findByGameId(gameId);
        List<PlatformAchievement> platformList = port.fetchAchievements(gameMapping.platformGameId());

        List<AchievementMapping> result = match(steamList, platformList, platform);
        tx.executeWithoutResult(status -> achievementMappings.saveAll(result));
        return (int) result.stream().filter(AchievementMapping::available).count();
    }

    private static List<AchievementMapping> match(List<Achievement> steamList,
                                                  List<PlatformAchievement> platformList, Platform platform) {
        Map<String, PlatformAchievement> byNameAndDescription = new HashMap<>();
        Map<String, List<PlatformAchievement>> byName = new HashMap<>();
        for (PlatformAchievement p : platformList) {
            byNameAndDescription.putIfAbsent(norm(p.name()) + "|" + norm(p.description()), p);
            byName.computeIfAbsent(norm(p.name()), k -> new ArrayList<>()).add(p);
        }

        Set<String> used = new HashSet<>();
        List<AchievementMapping> result = new ArrayList<>();
        for (Achievement steam : steamList) {
            PlatformAchievement match = byNameAndDescription.get(norm(steam.name()) + "|" + norm(steam.description()));
            if (match == null || used.contains(match.platformAchievementId())) {
                List<PlatformAchievement> sameName = byName.getOrDefault(norm(steam.name()), List.of());
                match = (sameName.size() == 1 && !used.contains(sameName.get(0).platformAchievementId()))
                        ? sameName.get(0) : null;
            }
            if (match == null) {
                result.add(AchievementMapping.unavailable(steam.id(), platform));
            } else {
                used.add(match.platformAchievementId());
                result.add(AchievementMapping.matched(steam.id(), platform, match.platformAchievementId()));
            }
        }
        return result;
    }

    private static String norm(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
