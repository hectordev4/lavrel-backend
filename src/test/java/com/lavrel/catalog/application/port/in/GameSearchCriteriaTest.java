package com.lavrel.catalog.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameSearchCriteria;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.Sort;
import java.util.List;
import org.junit.jupiter.api.Test;

class GameSearchCriteriaTest {

    private static GameSearchCriteria criteria(String q, List<Long> tags, Sort sort, int page, int size) {
        return new GameSearchCriteria(q, tags, null, null, null, null, sort, page, size);
    }

    @Test
    void withATextQueryTheDefaultIsRelevance() {
        assertThat(criteria(" witcher ", null, null, 0, 0).sort()).isEqualTo(Sort.RELEVANCE);
        assertThat(criteria(" witcher ", null, null, 0, 0).query()).isEqualTo("witcher");
    }

    @Test
    void withoutAQueryRelevanceMakesNoSenseSoPopularIsUsed() {
        assertThat(criteria(null, null, null, 0, 0).sort()).isEqualTo(Sort.POPULAR);
        assertThat(criteria("  ", null, Sort.RELEVANCE, 0, 0).sort()).isEqualTo(Sort.POPULAR);
        assertThat(criteria(null, null, Sort.NEWEST, 0, 0).sort()).isEqualTo(Sort.NEWEST);
    }

    @Test
    void pageAndSizeAreKeptInRange() {
        GameSearchCriteria c = criteria(null, null, null, -3, 500);
        assertThat(c.page()).isZero();
        assertThat(c.size()).isEqualTo(GameSearchCriteria.MAX_SIZE);
        assertThat(criteria(null, null, null, 0, 0).size()).isEqualTo(GameSearchCriteria.DEFAULT_SIZE);
    }

    @Test
    void tagsAreDistinctAndLimited() {
        GameSearchCriteria c = criteria(null, List.of(1L, 1L, 2L, 3L, 4L, 5L, 6L), null, 0, 0);
        assertThat(c.tagIds()).containsExactly(1L, 2L, 3L, 4L, 5L);
    }
}
