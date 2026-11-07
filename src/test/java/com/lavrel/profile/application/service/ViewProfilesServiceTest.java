package com.lavrel.profile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.profile.application.port.in.ViewProfilesUseCase;
import com.lavrel.profile.application.port.out.ProfileSettingsRepositoryPort;
import com.lavrel.profile.application.port.out.PublicProfileQueryPort;
import com.lavrel.profile.application.port.out.ShelfContentQueryPort;
import com.lavrel.profile.application.port.out.ShelfRepositoryPort;
import com.lavrel.profile.domain.AvatarKind;
import com.lavrel.profile.domain.ProfileNotFoundException;
import com.lavrel.profile.domain.ProfileSettings;
import com.lavrel.profile.domain.Shelf;
import com.lavrel.profile.domain.ShelfType;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ViewProfilesServiceTest {

    private final PublicProfileQueryPort query = mock(PublicProfileQueryPort.class);
    private final ShelfRepositoryPort shelves = mock(ShelfRepositoryPort.class);
    private final ShelfContentQueryPort content = mock(ShelfContentQueryPort.class);
    private final ProfileSettingsRepositoryPort settings = mock(ProfileSettingsRepositoryPort.class);
    private final ViewProfilesService service = new ViewProfilesService(query, shelves, content, settings);

    @Test
    void privateOrUnknownProfilesAreNotFound() {
        when(query.findHeader(1234567890123456L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.header(1234567890123456L)).isInstanceOf(ProfileNotFoundException.class);
    }

    @Test
    void sixteenDigitsSearchOneExactProfile() {
        service.search(" 1234567890123456 ", 0);
        verify(query).search("1234567890123456", 1234567890123456L, ViewProfilesService.PAGE_SIZE, 0L);
    }

    @Test
    void anythingElseSearchesByNamePageByPage() {
        service.search("otter", 2);
        verify(query).search("otter", null, ViewProfilesService.PAGE_SIZE, 2L * ViewProfilesService.PAGE_SIZE);
    }

    @Test
    void shelvesAreFilledInTheUsersOrder() {
        long publicId = 1234567890123456L;
        when(query.findHeader(publicId)).thenReturn(Optional.of(new ViewProfilesUseCase.ProfileHeader(publicId,
                "SwiftOtter", YearMonth.of(2026, 11), null, List.of(),
                ViewProfilesUseCase.Avatar.GENERATED)));
        when(query.findUserId(publicId)).thenReturn(Optional.of(7L));
        Instant t = Instant.EPOCH;
        when(shelves.findAll(7L)).thenReturn(List.of(
                Shelf.restore(1L, UUID.randomUUID(), 7L,
                        ShelfType.STATS, null, 1, null, List.of(), t),
                Shelf.restore(2L, UUID.randomUUID(), 7L,
                        ShelfType.RECENTLY_PLAYED, "Lately", 2, null, List.of(), t)));
        when(content.stats(7L)).thenReturn(new ViewProfilesUseCase.Stats(10, 200, 3));
        when(settings.find(7L)).thenReturn(Optional.of(ProfileSettings.restore(7L, true,
                false, false, false, true, AvatarKind.GENERATED, null)));
        when(content.recentlyPlayed(7L, ViewProfilesService.RECENT_GAMES)).thenReturn(List.of(
                new ViewProfilesUseCase.GameItem(5L, "Hades", null)));

        var profile = service.profile(publicId);

        assertThat(profile.shelves()).hasSize(2);
        assertThat(profile.shelves().get(0).stats().gamesOwned()).isEqualTo(10);
        assertThat(profile.shelves().get(1).games()).hasSize(1);
        assertThat(profile.shelves().get(1).title()).isEqualTo("Lately");
    }

    @Test
    void theNumberOfGamesOwnedStaysHiddenUnlessTheUserOptsIn() {
        long publicId = 1234567890123457L;
        when(query.findHeader(publicId)).thenReturn(Optional.of(new ViewProfilesUseCase.ProfileHeader(publicId,
                "SwiftOtter", YearMonth.of(2026, 11), null, List.of(),
                ViewProfilesUseCase.Avatar.GENERATED)));
        when(query.findUserId(publicId)).thenReturn(Optional.of(8L));
        when(shelves.findAll(8L)).thenReturn(List.of(Shelf.restore(1L,
                UUID.randomUUID(), 8L, ShelfType.STATS, null, 1, null,
                List.of(), Instant.EPOCH)));
        when(content.stats(8L)).thenReturn(new ViewProfilesUseCase.Stats(10, 200, 3));
        when(settings.find(8L)).thenReturn(Optional.empty());

        var stats = service.profile(publicId).shelves().get(0).stats();

        assertThat(stats.gamesOwned()).isNull();
        assertThat(stats.achievementsUnlocked()).isEqualTo(200);
    }
}
