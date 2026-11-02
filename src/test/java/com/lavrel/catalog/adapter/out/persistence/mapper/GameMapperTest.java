package com.lavrel.catalog.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.catalog.domain.Game;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class GameMapperTest {

    @Test
    void everyFieldSurvivesTheRoundTrip() {
        Game game = Game.restore(7L, "Marvel Rivals", "cover.jpg", "Hero shooter", "https://store/2767030",
                Instant.parse("2026-11-02T10:00:00Z"));

        Game back = GameMapper.toDomain(GameMapper.toEntity(game));

        assertThat(back).usingRecursiveComparison().isEqualTo(game);
    }
}
