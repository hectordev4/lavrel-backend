package com.lavrel.progress.application.service;

import com.lavrel.progress.application.port.in.PickUnlocksUseCase;
import com.lavrel.progress.application.port.out.PickUnlocksQueryPort;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PickUnlocksService implements PickUnlocksUseCase {

    private final PickUnlocksQueryPort query;

    public PickUnlocksService(PickUnlocksQueryPort query) {
        this.query = query;
    }

    @Override
    public GamesPage gamesWithUnlocks(long userId, String rawQuery, GameSort sort, int page, int size) {
        String text = rawQuery == null || rawQuery.isBlank() ? null : rawQuery.trim();
        int pageSize = size <= 0 ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        int pageNumber = Math.max(0, page);
        List<GameWithUnlocks> rows = query.gamesWithUnlocks(userId, text, sort == null ? GameSort.RECENT : sort,
                pageSize + 1, (long) pageNumber * pageSize);
        boolean hasMore = rows.size() > pageSize;
        return new GamesPage(hasMore ? rows.subList(0, pageSize) : rows, pageNumber, pageSize, hasMore);
    }

    @Override
    public List<UnlockedAchievement> unlockedIn(long userId, long gameId, AchievementSort sort) {
        return query.unlockedIn(userId, gameId, sort == null ? AchievementSort.RARITY : sort);
    }
}
