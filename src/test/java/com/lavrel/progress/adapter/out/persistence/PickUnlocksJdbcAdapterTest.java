package com.lavrel.progress.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.progress.application.port.in.PickUnlocksUseCase.AchievementSort;
import com.lavrel.progress.application.port.in.PickUnlocksUseCase.GameSort;
import org.junit.jupiter.api.Test;

class PickUnlocksJdbcAdapterTest {

    @Test
    void theNameFilterKeepsItsWildcardsAndTheOrderIsFilledIn() {
        assertThat(PickUnlocksJdbcAdapter.gamesSql(GameSort.RECENT))
                .contains("LIKE '%' || lavrel_normalize(:q) || '%'")
                .contains("ORDER BY last_unlock DESC NULLS LAST, g.id")
                .doesNotContain("ORDER_BY");
        assertThat(PickUnlocksJdbcAdapter.gamesSql(GameSort.NAME)).contains("ORDER BY g.search_name, g.id");
    }

    @Test
    void achievementOrders() {
        assertThat(PickUnlocksJdbcAdapter.unlockedSql(AchievementSort.RARITY))
                .contains("ORDER BY global_percent ASC NULLS LAST").doesNotContain("ORDER_BY");
        assertThat(PickUnlocksJdbcAdapter.unlockedSql(AchievementSort.DATE))
                .contains("ORDER BY ua.unlocked_at DESC NULLS LAST");
    }
}
