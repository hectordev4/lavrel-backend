package com.lavrel.progress.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.lavrel.progress.application.port.in.RecordUnlocksUseCase.Command;
import com.lavrel.progress.application.port.in.RecordUnlocksUseCase.Unlock;
import com.lavrel.progress.application.port.out.UserAchievementRepositoryPort;
import com.lavrel.progress.domain.UserAchievement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RecordUnlocksServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-15T10:00:00Z");
    private static final Instant UNLOCKED_A = Instant.parse("2024-03-01T18:30:00Z");
    private static final Instant UNLOCKED_B = Instant.parse("2025-07-12T09:15:00Z");

    private final UserAchievementRepositoryPort repository = mock(UserAchievementRepositoryPort.class);
    private final RecordUnlocksService service =
            new RecordUnlocksService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void nothingToRecordReturnsZeroWithoutTouchingTheRepository() {
        int recorded = service.record(new Command(7L, 5L, List.of()));

        assertThat(recorded).isZero();
        verifyNoInteractions(repository);
    }

    @Test
    void eachRowHasTheUserAchievementAccountUnlockTimeAndTheClockAsRecordedAt() {
        when(repository.insertMissing(anyCollection())).thenReturn(2);

        int recorded = service.record(new Command(7L, 5L,
                List.of(new Unlock(100L, UNLOCKED_A), new Unlock(200L, UNLOCKED_B))));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<UserAchievement>> rows = ArgumentCaptor.forClass(Collection.class);
        verify(repository).insertMissing(rows.capture());
        assertThat(rows.getValue()).containsExactly(
                new UserAchievement(7L, 100L, 5L, UNLOCKED_A, NOW),
                new UserAchievement(7L, 200L, 5L, UNLOCKED_B, NOW));
        assertThat(recorded).isEqualTo(2);
    }

    @Test
    void returnsHowManyWereReallyNew() {
        when(repository.insertMissing(anyCollection())).thenReturn(1);

        int recorded = service.record(new Command(7L, 5L,
                List.of(new Unlock(100L, UNLOCKED_A), new Unlock(200L, UNLOCKED_B))));

        assertThat(recorded).isEqualTo(1);
    }

    @Test
    void forgetAccountDeletesBySourceAccount() {
        when(repository.deleteBySourceAccount(7L, 5L)).thenReturn(3);

        int removed = service.forgetAccount(7L, 5L);

        verify(repository).deleteBySourceAccount(7L, 5L);
        assertThat(removed).isEqualTo(3);
    }
}
