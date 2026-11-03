package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase;
import com.lavrel.catalog.application.port.out.GameSearchQueryPort;
import org.springframework.stereotype.Service;

@Service
public class SearchGamesService implements SearchGamesUseCase {

    private final GameSearchQueryPort searchQuery;

    public SearchGamesService(GameSearchQueryPort searchQuery) {
        this.searchQuery = searchQuery;
    }

    @Override
    public GamePage search(GameSearchCriteria criteria) {
        return searchQuery.search(criteria);
    }
}
