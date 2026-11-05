package com.lavrel.profile.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.profile.application.port.out.PublicProfileQueryPort;
import com.lavrel.profile.application.port.out.ShelfContentQueryPort;
import com.lavrel.profile.application.port.out.ShelfRepositoryPort;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase;
import com.lavrel.profile.domain.ProfileNotFoundException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ViewProfilesServiceTest {

    private final PublicProfileQueryPort query = mock(PublicProfileQueryPort.class);
    private final ShelfRepositoryPort shelves = mock(ShelfRepositoryPort.class);
    private final ShelfContentQueryPort content = mock(ShelfContentQueryPort.class);
    private final ViewProfilesService service = new ViewProfilesService(query, shelves, content);

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
                "SwiftOtter", java.time.YearMonth.of(2026, 11), null, java.util.List.of(),
                ViewProfilesUseCase.Avatar.GENERATED)));
        when(query.findUserId(publicId)).thenReturn(Optional.of(7L));
        java.time.Instant t = java.time.Instant.EPOCH;
        when(shelves.findAll(7L)).thenReturn(java.util.List.of(
                com.lavrel.profile.domain.Shelf.restore(1L, java.util.UUID.randomUUID(), 7L,
                        com.lavrel.profile.domain.ShelfType.STATS, null, 1, null, java.util.List.of(), t),
                com.lavrel.profile.domain.Shelf.restore(2L, java.util.UUID.randomUUID(), 7L,
                        com.lavrel.profile.domain.ShelfType.RECENTLY_PLAYED, "Lately", 2, null, java.util.List.of(), t)));
        when(content.stats(7L)).thenReturn(new ViewProfilesUseCase.Stats(10, 200, 3));
        when(content.recentlyPlayed(7L, ViewProfilesService.RECENT_GAMES)).thenReturn(java.util.List.of(
                new ViewProfilesUseCase.GameItem(5L, "Hades", null)));

        var profile = service.profile(publicId);

        org.assertj.core.api.Assertions.assertThat(profile.shelves()).hasSize(2);
        org.assertj.core.api.Assertions.assertThat(profile.shelves().get(0).stats().gamesOwned()).isEqualTo(10);
        org.assertj.core.api.Assertions.assertThat(profile.shelves().get(1).games()).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(profile.shelves().get(1).title()).isEqualTo("Lately");
    }
}
