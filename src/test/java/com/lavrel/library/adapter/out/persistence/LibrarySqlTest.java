package com.lavrel.library.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase.PlayerMode;
import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryCriteria;
import com.lavrel.library.application.port.in.GetLibraryUseCase.Sort;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LibrarySqlTest {

    @Test
    void onlyTheUsersOwnPublishedGames() {
        LibrarySql.Query q = LibrarySql.build(7L, new LibraryCriteria(null, null, null, null, null, 0, 48));
        assertThat(q.sql()).contains("pa.user_id = :userId").contains("g.enriched_at IS NOT NULL");
        assertThat(q.params().getValue("userId")).isEqualTo(7L);
        assertThat(q.sql()).contains("o.last_played DESC NULLS LAST");
    }

    @Test
    void filtersAreTheSameAsSearch() {
        LibrarySql.Query q = LibrarySql.build(7L, new LibraryCriteria("witcher", List.of(19L, 492L),
                Set.of(PlayerMode.CO_OP), null, Sort.PROGRESS, 1, 48));
        assertThat(q.sql()).contains("lavrel_normalize(:q)").contains(":tag0").contains(":tag1").contains(":mode0")
                .contains("NULLIF(ach.total, 0)");
        assertThat(q.params().getValue("limit")).isEqualTo(49);
        assertThat(q.params().getValue("offset")).isEqualTo(48L);
    }

    @Test
    void collectionFilterOnlyMatchesTheUsersOwnCollection() {
        UUID id = UUID.randomUUID();
        LibrarySql.Query q = LibrarySql.build(7L, new LibraryCriteria(null, null, null, id, null, 0, 48));
        assertThat(q.sql()).contains("c.public_id = :collectionId AND c.user_id = :userId");
        assertThat(q.params().getValue("collectionId")).isEqualTo(id);
    }
}
