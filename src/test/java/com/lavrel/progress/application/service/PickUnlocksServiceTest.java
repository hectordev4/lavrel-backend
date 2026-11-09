package com.lavrel.progress.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.progress.application.port.in.PickUnlocksUseCase;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.AchievementSort;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.GameSort;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.GameWithUnlocks;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.GamesPage;
import com.lavrel.progress.application.port.out.PickUnlocksQueryPort;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PickUnlocksServiceTest {

    private final PickUnlocksQueryPort query = mock(PickUnlocksQueryPort.class);
    private final PickUnlocksService service = new PickUnlocksService(query);

    @Test
    void oneExtraRowTellsThereIsAnotherPage() {
        List<GameWithUnlocks> rows = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            rows.add(new GameWithUnlocks(i, "G" + i, "g" + i, null, 1, 10, null));
        }
        when(query.gamesWithUnlocks(1L, "hades", GameSort.RECENT, 3, 2L)).thenReturn(rows);

        GamesPage page = service.gamesWithUnlocks(1L, "  hades ", null, 1, 2);

        assertThat(page.games()).hasSize(2);
        assertThat(page.hasMore()).isTrue();
    }

    @Test
    void blankSearchMeansNoFilterAndSizeIsCapped() {
        service.gamesWithUnlocks(1L, "  ", GameSort.NAME, 0, 1000);

        verify(query).gamesWithUnlocks(1L, null, GameSort.NAME, PickUnlocksUseCase.MAX_PAGE_SIZE + 1, 0L);
    }

    @Test
    void rarestFirstByDefault() {
        service.unlockedIn(1L, 7L, null);

        verify(query).unlockedIn(1L, 7L, AchievementSort.RARITY);
    }
}
