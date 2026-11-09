package com.lavrel.profile.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.library.application.port.in.ManageCollectionsUseCase;
import com.lavrel.library.application.port.in.ManageCollectionsUseCase.CollectionView;
import com.lavrel.profile.application.port.in.ManageShelvesUseCase;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.AchievementItem;
import com.lavrel.profile.application.port.out.ShelfContentQueryPort;
import com.lavrel.profile.application.port.out.ShelfRepositoryPort;
import com.lavrel.profile.domain.InvalidShelfException;
import com.lavrel.profile.domain.Shelf;
import com.lavrel.profile.domain.ShelfLimitReachedException;
import com.lavrel.profile.domain.ShelfType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ManageShelvesServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");

    private final ShelfRepositoryPort shelves = mock(ShelfRepositoryPort.class);
    private final ShelfContentQueryPort content = mock(ShelfContentQueryPort.class);
    private final ManageCollectionsUseCase collections = mock(ManageCollectionsUseCase.class);
    private final ManageShelvesService service =
            new ManageShelvesService(shelves, content, collections, Clock.fixed(NOW, ZoneOffset.UTC));

    private static Shelf stored(long id, ShelfType type, int position) {
        return Shelf.restore(id, UUID.randomUUID(), 1L, type, null, position, null, List.of(), NOW);
    }

    @Test
    void newShelfGoesAtTheEnd() {
        when(shelves.findAll(1L)).thenReturn(List.of(stored(1L, ShelfType.STATS, 1)));
        when(shelves.save(any())).thenAnswer(i -> i.getArgument(0));

        var view = service.add(1L, ShelfType.RECENTLY_PLAYED, " Lately ", null);

        assertThat(view.position()).isEqualTo(2);
        assertThat(view.title()).isEqualTo("Lately");
    }

    @Test
    void oneOnlyShelvesCannotRepeat() {
        when(shelves.findAll(1L)).thenReturn(List.of(stored(1L, ShelfType.STATS, 1)));

        assertThatThrownBy(() -> service.add(1L, ShelfType.STATS, null, null))
                .isInstanceOf(InvalidShelfException.class);
    }

    @Test
    void onlyMyOwnCollectionsCanBeShown() {
        when(shelves.findAll(1L)).thenReturn(List.of());
        when(collections.list(1L)).thenReturn(List.of(new CollectionView(UUID.randomUUID(), "Mine", 1, 3)));

        assertThatThrownBy(() -> service.add(1L, ShelfType.COLLECTION, null, UUID.randomUUID()))
                .isInstanceOf(InvalidShelfException.class);
        verify(shelves, never()).save(any());
    }

    @Test
    void tenShelvesAtMost() {
        List<Shelf> full = new ArrayList<>();
        for (int i = 1; i <= ManageShelvesUseCase.MAX_SHELVES; i++) {
            full.add(stored(i, ShelfType.ACHIEVEMENTS, i));
        }
        when(shelves.findAll(1L)).thenReturn(full);

        assertThatThrownBy(() -> service.add(1L, ShelfType.ACHIEVEMENTS, null, null))
                .isInstanceOf(ShelfLimitReachedException.class);
    }

    @Test
    void onlyUnlockedAchievementsCanBeShowcased() {
        Shelf shelf = stored(4L, ShelfType.ACHIEVEMENTS, 1);
        when(shelves.find(1L, shelf.publicId())).thenReturn(Optional.of(shelf));
        when(content.unlockedAmong(1L, List.of(10L, 11L))).thenReturn(Set.of(10L));

        assertThatThrownBy(() -> service.showAchievements(1L, shelf.publicId(), List.of(10L, 11L)))
                .isInstanceOf(InvalidShelfException.class);
        verify(shelves, never()).save(any());
    }

    @Test
    void deletingClosesTheGap() {
        Shelf first = stored(1L, ShelfType.STATS, 1);
        Shelf third = stored(3L, ShelfType.PLAYING, 3);
        when(shelves.find(1L, first.publicId())).thenReturn(Optional.of(first));
        when(shelves.findAll(1L)).thenReturn(List.of(third));

        service.delete(1L, first.publicId());

        verify(shelves).delete(1L);
        assertThat(third.position()).isEqualTo(1);
        verify(shelves).saveOrder(List.of(third));
        verify(content, never()).unlockedAmong(anyLong(), any());
    }

    @Test
    void myAchievementShelvesComeWithTheirPickedAchievements() {
        Shelf shelf = Shelf.restore(4L, UUID.randomUUID(), 1L, ShelfType.ACHIEVEMENTS, "Proud", 1, null,
                List.of(10L, 11L), NOW);
        when(shelves.findAll(1L)).thenReturn(List.of(shelf, stored(5L, ShelfType.STATS, 2)));
        when(content.achievements(1L, List.of(10L, 11L))).thenReturn(List.of(
                new AchievementItem(10L, "Phoenix Rising", "icon", 7L, "Hades", "hades", 2.3, NOW)));

        var views = service.list(1L);

        assertThat(views.get(0).achievements()).extracting(AchievementItem::name).containsExactly("Phoenix Rising");
        assertThat(views.get(0).achievementIds()).containsExactly(10L, 11L);
        assertThat(views.get(1).achievements()).isEmpty();
    }
}
