package com.lavrel.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.CreatorRole;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GamePage;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameSearchCriteria;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.Sort;
import com.lavrel.catalog.application.port.out.FilterOptionsQueryPort;
import com.lavrel.catalog.application.port.out.GameSearchQueryPort;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class QuickSearchServiceTest {

    private final GameSearchQueryPort games = mock(GameSearchQueryPort.class);
    private final FilterOptionsQueryPort options = mock(FilterOptionsQueryPort.class);
    private final QuickSearchService service = new QuickSearchService(games, options);

    @Test
    void asksForAFewOfEachKindRankedByRelevance() {
        when(games.search(any())).thenReturn(new GamePage(List.of(), 0, 5, false));

        service.search("  witch ");

        ArgumentCaptor<GameSearchCriteria> criteria = ArgumentCaptor.forClass(GameSearchCriteria.class);
        verify(games).search(criteria.capture());
        assertThat(criteria.getValue().size()).isEqualTo(QuickSearchService.GAMES);
        assertThat(criteria.getValue().sort()).isEqualTo(Sort.RELEVANCE);
        verify(options).tags("witch", QuickSearchService.TAGS);
        verify(options).creators(CreatorRole.FRANCHISE, "witch", QuickSearchService.FRANCHISES);
    }
}
