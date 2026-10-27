package com.lavrel.catalog.application.service.steam;

import static com.lavrel.catalog.application.service.steam.SteamTestData.CLOCK;
import static com.lavrel.catalog.application.service.steam.SteamTestData.NOW;
import static com.lavrel.catalog.application.service.steam.SteamTestData.game;
import static com.lavrel.catalog.application.service.steam.SteamTestData.gameInfo;
import static com.lavrel.catalog.application.service.steam.SteamTestData.oneAchievement;
import static com.lavrel.catalog.application.service.steam.SteamTestData.storeItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.ImportFailureRepositoryPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.shared.domain.Platform;
import com.lavrel.shared.domain.PlatformUnavailableException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SteamImportRunTest {

    private final PlatformCatalogPort steam = mock(PlatformCatalogPort.class);
    private final GameMappingRepositoryPort mappings = mock(GameMappingRepositoryPort.class);
    private final ImportFailureRepositoryPort failures = mock(ImportFailureRepositoryPort.class);
    private final SteamGameWriter writer = mock(SteamGameWriter.class);
    private SteamImportRun importRun;

    @BeforeEach
    void setUp() {
        when(steam.platform()).thenReturn(Platform.STEAM);
        when(mappings.findByPlatformAndPlatformGameIds(eq(Platform.STEAM), any())).thenReturn(List.of());
        when(failures.findFailedSince(eq(Platform.STEAM), any(), any(), any())).thenReturn(Set.of());
        importRun = new SteamImportRun(List.of(steam), mappings, failures, SteamTestData.screening(), writer,
                CLOCK, 30);
    }

    private void goodGame(String appId, long gameId) {
        when(steam.fetchAchievements(appId)).thenReturn(oneAchievement());
        when(steam.fetchGameInfo(appId)).thenReturn(Optional.of(gameInfo(appId)));
        when(writer.insert(eq(appId), any(), any(), any())).thenReturn(gameId);
    }

    @Test
    void knownAppsAndRememberedFailuresCostNoSteamCall() {
        when(mappings.findByPlatformAndPlatformGameIds(eq(Platform.STEAM), any()))
                .thenReturn(List.of(new GameMapping(1L, 100L, Platform.STEAM, "10", "Known")));
        when(failures.findFailedSince(eq(Platform.STEAM), any(), any(), any())).thenReturn(Set.of("20"));
        when(steam.fetchStoreItems(List.of("30"))).thenReturn(Map.of("30", storeItem("30", "dlc", 1000, List.of())));

        SteamImportRun.Result result = importRun.run(List.of("10", "20", "30"), 10, ImportMode.SYNC, 0);

        verify(steam).fetchStoreItems(List.of("30"));
        assertThat(result.alreadyKnown()).isEqualTo(1);
        assertThat(result.skippedKnownFailures()).isEqualTo(1);
        assertThat(result.notImportable()).isEqualTo(1);
        assertThat(result.attempts()).isZero();
        verify(failures).recordFailure(Platform.STEAM, "30", "type: dlc", NOW);
    }

    @Test
    void safeGameWithAchievementsIsImported() {
        when(steam.fetchStoreItems(List.of("30"))).thenReturn(Map.of("30", game("30")));
        goodGame("30", 7L);

        SteamImportRun.Result result = importRun.run(List.of("30"), 10, ImportMode.SYNC, 0);

        assertThat(result.imported()).containsEntry("30", 7L);
        assertThat(result.attempts()).isEqualTo(1);
        verify(failures, never()).recordFailure(any(), anyString(), anyString(), any());
    }

    @Test
    void gameWithoutAchievementsIsRememberedBeforeTheStorePageCall() {
        when(steam.fetchStoreItems(List.of("30"))).thenReturn(Map.of("30", game("30")));
        when(steam.fetchAchievements("30")).thenReturn(List.of());

        SteamImportRun.Result result = importRun.run(List.of("30"), 10, ImportMode.SYNC, 0);

        assertThat(result.notImportable()).isEqualTo(1);
        verify(failures).recordFailure(Platform.STEAM, "30", SteamGameScreening.NO_ACHIEVEMENTS, NOW);
        verify(steam, never()).fetchGameInfo(anyString());
    }

    @Test
    void attemptLimitDefersTheRestAndReportsWhereToContinue() {
        when(steam.fetchStoreItems(List.of("1", "2", "3")))
                .thenReturn(Map.of("1", game("1"), "2", game("2"), "3", game("3")));
        goodGame("1", 11L);

        SteamImportRun.Result result = importRun.run(List.of("1", "2", "3"), 1, ImportMode.BULK, 0);

        assertThat(result.attempts()).isEqualTo(1);
        assertThat(result.budgetExhausted()).isTrue();
        assertThat(result.deferred()).isEqualTo(2);
        assertThat(result.lastProcessedId()).isEqualTo("1");
    }

    @Test
    void bulkModeRemembersUnpopularGames() {
        when(steam.fetchStoreItems(List.of("30"))).thenReturn(Map.of("30", storeItem("30", "game", 5, List.of())));

        SteamImportRun.Result result = importRun.run(List.of("30"), 10, ImportMode.BULK, 0);

        assertThat(result.attempts()).isZero();
        verify(failures).recordFailure(Platform.STEAM, "30", SteamGameScreening.TOO_FEW_REVIEWS, NOW);
    }

    @Test
    void steamOutageStopsTheRunWithoutRememberingFailures() {
        when(steam.fetchStoreItems(any())).thenThrow(new PlatformUnavailableException(Platform.STEAM, "down"));

        SteamImportRun.Result result = importRun.run(List.of("1", "2"), 10, ImportMode.SYNC, 0);

        assertThat(result.stoppedEarly()).isTrue();
        assertThat(result.deferred()).isEqualTo(2);
        verify(failures, never()).recordFailure(any(), anyString(), anyString(), any());
    }
}
