package com.lavrel.catalog.application.service.steam;

import static com.lavrel.catalog.application.service.steam.SteamTestData.game;
import static com.lavrel.catalog.application.service.steam.SteamTestData.gameInfo;
import static com.lavrel.catalog.application.service.steam.SteamTestData.storeItem;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SteamGameScreeningTest {

    private final SteamGameScreening screening = SteamTestData.screening();

    @Test
    void missingStoreItemIsNotFound() {
        assertThat(screening.cheapRejection("1", null, false)).contains(SteamGameScreening.NOT_FOUND);
    }

    @Test
    void nonGamesAreRejected() {
        assertThat(screening.cheapRejection("1", storeItem("1", "dlc", 1000, List.of()), false)).contains("type: dlc");
    }

    @Test
    void adultOnlyDescriptorIsRejectedOnTheCheapData() {
        assertThat(screening.cheapRejection("1", storeItem("1", "game", 1000, List.of(3)), false)).isPresent();
    }

    @Test
    void reviewCountOnlyMattersWhenPopularityIsRequired() {
        var unpopular = storeItem("1", "game", 10, List.of());
        assertThat(screening.cheapRejection("1", unpopular, true)).contains(SteamGameScreening.TOO_FEW_REVIEWS);
        assertThat(screening.cheapRejection("1", unpopular, false)).isEmpty();
    }

    @Test
    void safePopularGamePasses() {
        assertThat(screening.cheapRejection("1", game("1"), true)).isEmpty();
        assertThat(screening.fullRejection("1", gameInfo("1"), game("1"))).isEmpty();
    }

    @Test
    void fullCheckSeesTheStorePageType() {
        assertThat(screening.fullRejection("1", gameInfo("1", "music", Map.of()), game("1"))).contains("type: music");
    }

    @Test
    void fullCheckSeesExcludedGenres() {
        assertThat(screening.fullRejection("1", gameInfo("1", "game", Map.of(99, "Hentai")), game("1"))).isPresent();
    }
}
