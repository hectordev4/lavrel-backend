package com.lavrel.catalog.application.port.in;

import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.CreatorOption;
import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.TagOption;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameCard;
import java.util.List;

/** The type-ahead in the header: a few games, tags and franchises for what the user has typed so far. Public. */
public interface QuickSearchUseCase {

    QuickResults search(String query);

    record QuickResults(List<GameCard> games, List<TagOption> tags, List<CreatorOption> franchises) {
    }
}
