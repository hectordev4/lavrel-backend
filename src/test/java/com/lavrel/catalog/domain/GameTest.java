package com.lavrel.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class GameTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    @Test
    void createdGameHasNoIdAndAnEmptyMissingDescription() {
        Game game = Game.create("Marvel Rivals", "cover.jpg", null, "https://store/2767030", NOW);
        assertThat(game.id()).isNull();
        assertThat(game.description()).isEmpty();
    }

    @Test
    void refreshKeepsIdAndCreationTime() {
        Game game = Game.restore(7L, "Old", null, "old", "url", NOW);
        game.refreshFromPlatform("New", "cover.jpg", null, "url2");
        assertThat(game.id()).isEqualTo(7L);
        assertThat(game.createdAt()).isEqualTo(NOW);
        assertThat(game.title()).isEqualTo("New");
        assertThat(game.description()).isEmpty();
    }

    @Test
    void storedGamesAreEqualByIdAndNewOnesOnlyToThemselves() {
        Game a = Game.restore(7L, "A", null, "", null, NOW);
        Game b = Game.restore(7L, "B", null, "", null, NOW);
        Game fresh = Game.create("A", null, "", null, NOW);
        Game otherFresh = Game.create("A", null, "", null, NOW);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(fresh).isEqualTo(fresh).isNotEqualTo(otherFresh);
    }
}
