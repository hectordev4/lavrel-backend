package com.lavrel.catalog.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameSearchCriteria;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.PlayerMode;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.Sort;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GameSearchSqlTest {

    @Test
    void onlyPublishedGamesAreEverSearched() {
        GameSearchSql.Query q = GameSearchSql.build(
                new GameSearchCriteria(null, null, null, null, null, null, null, null, 0, 20));
        assertThat(q.sql()).contains("g.enriched_at IS NOT NULL").doesNotContain("q.v");
    }

    @Test
    void textIsNormalizedInTheDatabaseAndBoundAsAParameter() {
        GameSearchSql.Query q = GameSearchSql.build(
                new GameSearchCriteria("Pokémon'; DROP", null, null, null, null, null, null, null, 0, 20));
        assertThat(q.sql()).contains("lavrel_normalize(:q)").doesNotContain("DROP");
        assertThat(q.params().getValue("q")).isEqualTo("Pokémon'; DROP");
        assertThat(q.sql()).contains("word_similarity");
    }

    @Test
    void everyTagAndEveryModeIsAnAndCondition() {
        GameSearchSql.Query q = GameSearchSql.build(new GameSearchCriteria(null, List.of(19L, 492L), 7L, null, null,
                Set.of(PlayerMode.CO_OP, PlayerMode.SINGLE_PLAYER), null, null, 0, 20));
        assertThat(q.sql()).contains(":tag0").contains(":tag1").contains(":franchiseId").contains("'FRANCHISE'")
                .contains(":mode0").contains(":mode1");
        assertThat(q.params().getValue("tag1")).isEqualTo(492L);
    }

    @Test
    void oneExtraRowIsFetchedToKnowIfThereIsANextPage() {
        GameSearchSql.Query q = GameSearchSql.build(
                new GameSearchCriteria(null, null, null, null, null, null, null, Sort.NEWEST, 2, 20));
        assertThat(q.params().getValue("limit")).isEqualTo(21);
        assertThat(q.params().getValue("offset")).isEqualTo(40L);
        assertThat(q.sql()).contains("release_date DESC NULLS LAST");
    }

    @Test
    void everySystemMustMatchAndCardsListTheirSystems() {
        GameSearchSql.Query q = GameSearchSql.build(new GameSearchCriteria(null, null, null, null, null, null,
                java.util.Set.of(com.lavrel.catalog.domain.GameSystem.XBOX), null, 0, 20));
        assertThat(q.sql()).contains(":system0").contains("AS systems");
        assertThat(q.params().getValue("system0")).isEqualTo("XBOX");
    }
}
