package com.lavrel.catalog.application.port.out;

import com.lavrel.catalog.domain.Achievement;
import java.util.List;

public interface AchievementRepositoryPort {

    List<Achievement> findByGameId(long gameId);

    /** Upserts by (gameId, steamAchievementId). */
    List<Achievement> saveAll(List<Achievement> achievements);
}
