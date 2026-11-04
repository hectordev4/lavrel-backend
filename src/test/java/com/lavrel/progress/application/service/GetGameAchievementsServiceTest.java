package com.lavrel.progress.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.catalog.application.port.in.CheckPublishedGameUseCase;
import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.progress.application.port.in.GetGameAchievementsUseCase.AchievementItem;
import com.lavrel.progress.application.port.out.AchievementStatsQueryPort;
import com.lavrel.progress.application.port.out.GameAchievementsQueryPort;
import com.lavrel.progress.application.port.out.GameAchievementsQueryPort.AchievementRow;
import com.lavrel.progress.domain.AchievementStats;
import com.lavrel.user.application.port.in.GetUserCountryUseCase;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class GetGameAchievementsServiceTest {

    private static final Instant T = Instant.parse("2026-11-04T10:00:00Z");

    private final CheckPublishedGameUseCase catalog = mock(CheckPublishedGameUseCase.class);
    private final GameAchievementsQueryPort achievements = mock(GameAchievementsQueryPort.class);
    private final AchievementStatsQueryPort stats = mock(AchievementStatsQueryPort.class);
    private final GetUserCountryUseCase country = mock(GetUserCountryUseCase.class);
    private final GetGameAchievementsService service =
            new GetGameAchievementsService(catalog, achievements, stats, country, 20);

    @Test
    void signedInUserGetsUnlocksAndCountryPercentages() {
        when(country.getCountryId(1L)).thenReturn(34);
        when(stats.findGameStats(7L, 34, 20)).thenReturn(List.of(new AchievementStats(100L, 64.2, 34, 70.0)));
        when(achievements.findAchievements(7L, 1L)).thenReturn(List.of(
                new AchievementRow(100L, "First", "Do it", null, false, T),
                new AchievementRow(101L, "Secret", "???", null, true, null)));

        List<AchievementItem> items = service.list(7L, 1L);

        assertThat(items.get(0).globalPercent()).isEqualTo(64.2);
        assertThat(items.get(0).countryPercent()).isEqualTo(70.0);
        assertThat(items.get(0).unlockedAt()).isEqualTo(T);
        assertThat(items.get(1).globalPercent()).isNull();
        assertThat(items.get(1).hidden()).isTrue();
    }

    @Test
    void anonymousVisitorsGetTheListWithoutCountryOrUnlocks() {
        when(stats.findGameStats(7L, null, 20)).thenReturn(List.of());
        when(achievements.findAchievements(7L, null)).thenReturn(List.of(
                new AchievementRow(100L, "First", "Do it", null, false, null)));

        assertThat(service.list(7L, null)).hasSize(1);
        verify(country, never()).getCountryId(anyLong());
    }

    @Test
    void unpublishedGamesAreNotFound() {
        doThrow(new GameNotFoundException(9L)).when(catalog).requirePublished(9L);

        assertThatThrownBy(() -> service.list(9L, null)).isInstanceOf(GameNotFoundException.class);
    }
}
