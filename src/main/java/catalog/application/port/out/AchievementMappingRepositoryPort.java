package catalog.application.port.out;

import catalog.domain.AchievementMapping;
import java.util.Collection;
import java.util.List;
import shared.domain.Platform;

public interface AchievementMappingRepositoryPort {

    /** Mappings of a game's achievements on one platform, filtered by platform achievement IDs. */
    List<AchievementMapping> findByGameAndPlatform(long gameId, Platform platform,
                                                   Collection<String> platformAchievementIds);

    /** Upserts by (achievementId, platform). */
    List<AchievementMapping> saveAll(List<AchievementMapping> mappings);
}
