package com.lavrel.profile.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class ShelfTest {

    private static final Instant T = Instant.EPOCH;

    @Test
    void onlyCollectionShelvesPointAtACollection() {
        assertThatThrownBy(() -> Shelf.create(1L, ShelfType.COLLECTION, null, 1, null, T))
                .isInstanceOf(InvalidShelfException.class);
        assertThatThrownBy(() -> Shelf.create(1L, ShelfType.STATS, null, 1, UUID.randomUUID(), T))
                .isInstanceOf(InvalidShelfException.class);
        assertThat(Shelf.create(1L, ShelfType.COLLECTION, null, 1, UUID.randomUUID(), T).collectionId()).isNotNull();
    }

    @Test
    void achievementShelvesTakeUpToTwelveDistinctOnes() {
        Shelf shelf = Shelf.create(1L, ShelfType.ACHIEVEMENTS, null, 1, null, T);
        shelf.showAchievements(List.of(3L, 1L, 2L));
        assertThat(shelf.achievementIds()).containsExactly(3L, 1L, 2L);
        assertThatThrownBy(() -> shelf.showAchievements(List.of(1L, 1L))).isInstanceOf(InvalidShelfException.class);
        assertThatThrownBy(() -> shelf.showAchievements(LongStream.rangeClosed(1, 13).boxed().toList()))
                .isInstanceOf(InvalidShelfException.class);
    }

    @Test
    void otherShelvesDoNotTakeAchievements() {
        Shelf shelf = Shelf.create(1L, ShelfType.COMPLETED, null, 1, null, T);
        assertThatThrownBy(() -> shelf.showAchievements(List.of(1L))).isInstanceOf(InvalidShelfException.class);
    }

    @Test
    void titlesAreOptionalAndLimited() {
        assertThat(ShelfTitles.validate("   ")).isNull();
        assertThat(ShelfTitles.validate("  Platinum   trophies ")).isEqualTo("Platinum trophies");
        assertThatThrownBy(() -> ShelfTitles.validate("x".repeat(61))).isInstanceOf(InvalidShelfException.class);
    }
}
