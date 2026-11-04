package com.lavrel.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AchievementTest {

    @Test
    void overlongTextsFromSteamAreClippedNotRejected() {
        Achievement a = new Achievement(null, 1L, "ACH_1", "n".repeat(3000), "d".repeat(20000), null, false,
                Instant.EPOCH);
        assertThat(a.name()).hasSize(Achievement.MAX_NAME);
        assertThat(a.description()).hasSize(Achievement.MAX_DESCRIPTION);
        assertThat(a.steamAchievementId()).isEqualTo("ACH_1");
    }

    @Test
    void neverCutsAnEmojiInHalf() {
        String text = "x".repeat(Achievement.MAX_NAME - 1) + "🏆";
        Achievement a = new Achievement(null, 1L, "ACH_1", text, null, null, false, Instant.EPOCH);
        assertThat(a.name()).hasSize(Achievement.MAX_NAME - 1);
    }

    @Test
    void normalTextsAreUntouched() {
        Achievement a = new Achievement(null, 1L, "ACH_1", "First Blood", "Win once", "https://icon", true,
                Instant.EPOCH);
        assertThat(a.name()).isEqualTo("First Blood");
        assertThat(a.description()).isEqualTo("Win once");
    }
}
