package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.CreatorRole;
import com.lavrel.catalog.application.port.in.QuickSearchUseCase;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameSearchCriteria;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.Sort;
import com.lavrel.catalog.application.port.out.FilterOptionsQueryPort;
import com.lavrel.catalog.application.port.out.GameSearchQueryPort;
import org.springframework.stereotype.Service;

/** Reuses the full search for the games, so both rank names the same way. */
@Service
public class QuickSearchService implements QuickSearchUseCase {

    static final int GAMES = 5;
    static final int TAGS = 5;
    static final int FRANCHISES = 3;

    private final GameSearchQueryPort games;
    private final FilterOptionsQueryPort options;

    public QuickSearchService(GameSearchQueryPort games, FilterOptionsQueryPort options) {
        this.games = games;
        this.options = options;
    }

    @Override
    public QuickResults search(String query) {
        GameSearchCriteria criteria = new GameSearchCriteria(query, null, null, null, null, null, null, Sort.RELEVANCE, 0,
                GAMES);
        return new QuickResults(games.search(criteria).games(), options.tags(criteria.query(), TAGS),
                options.creators(CreatorRole.FRANCHISE, criteria.query(), FRANCHISES));
    }
}
