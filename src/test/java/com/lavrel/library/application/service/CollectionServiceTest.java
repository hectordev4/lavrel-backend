package com.lavrel.library.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.library.application.port.in.ManageCollectionsUseCase;
import com.lavrel.library.application.port.out.CollectionRepositoryPort;
import com.lavrel.library.application.port.out.GameOwnershipPort;
import com.lavrel.library.domain.Collection;
import com.lavrel.library.domain.CollectionLimitReachedException;
import com.lavrel.library.domain.CollectionNameTakenException;
import com.lavrel.library.domain.CollectionNotFoundException;
import com.lavrel.library.domain.GameNotOwnedException;
import com.lavrel.library.domain.InvalidCollectionOrderException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CollectionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    private final CollectionRepositoryPort repo = mock(CollectionRepositoryPort.class);
    private final GameOwnershipPort ownership = mock(GameOwnershipPort.class);
    private final CollectionService service = new CollectionService(repo, ownership, Clock.fixed(NOW, ZoneOffset.UTC));

    private static Collection stored(long id, String name, int position) {
        return Collection.restore(id, UUID.randomUUID(), 1L, name, position, NOW);
    }

    @BeforeEach
    void setUp() {
        when(repo.visibleGameCounts(1L)).thenReturn(Map.of());
    }

    @Test
    void newCollectionGoesAtTheEnd() {
        when(repo.findAll(1L)).thenReturn(List.of(stored(1L, "A", 1)));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));

        var view = service.create(1L, "  Cozy games ");

        assertThat(view.name()).isEqualTo("Cozy games");
        assertThat(view.position()).isEqualTo(2);
    }

    @Test
    void namesAreUniquePerUser() {
        when(repo.findAll(1L)).thenReturn(List.of());
        when(repo.nameTaken(1L, "Cozy", null)).thenReturn(true);

        assertThatThrownBy(() -> service.create(1L, "Cozy")).isInstanceOf(CollectionNameTakenException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void fiftyIsTheLimit() {
        List<Collection> full = new ArrayList<>();
        for (int i = 1; i <= ManageCollectionsUseCase.MAX_COLLECTIONS; i++) {
            full.add(stored(i, "C" + i, i));
        }
        when(repo.findAll(1L)).thenReturn(full);

        assertThatThrownBy(() -> service.create(1L, "One more")).isInstanceOf(CollectionLimitReachedException.class);
    }

    @Test
    void onlyOwnedGamesCanBeAdded() {
        Collection c = stored(5L, "Cozy", 1);
        when(repo.find(1L, c.publicId())).thenReturn(Optional.of(c));
        when(ownership.owns(1L, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service.addGame(1L, c.publicId(), 99L)).isInstanceOf(GameNotOwnedException.class);
        verify(repo, never()).addGame(anyLong(), anyLong(), any());
    }

    @Test
    void ownedGameIsAdded() {
        Collection c = stored(5L, "Cozy", 1);
        when(repo.find(1L, c.publicId())).thenReturn(Optional.of(c));
        when(ownership.owns(1L, 7L)).thenReturn(true);

        service.addGame(1L, c.publicId(), 7L);

        verify(repo).addGame(eq(5L), eq(7L), eq(NOW));
    }

    @Test
    void someoneElsesCollectionIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repo.find(1L, id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rename(1L, id, "x")).isInstanceOf(CollectionNotFoundException.class);
    }

    @Test
    void deletingClosesTheGap() {
        Collection first = stored(1L, "A", 1);
        Collection third = stored(3L, "C", 3);
        when(repo.find(1L, first.publicId())).thenReturn(Optional.of(first));
        when(repo.findAll(1L)).thenReturn(List.of(third));

        service.delete(1L, first.publicId());

        verify(repo).delete(1L);
        verify(repo).saveAll(List.of(third));
        assertThat(third.position()).isEqualTo(1);
    }

    @Test
    void reorderMustListEveryCollectionOnce() {
        Collection a = stored(1L, "A", 1);
        Collection b = stored(2L, "B", 2);
        when(repo.findAll(1L)).thenReturn(List.of(a, b));

        assertThatThrownBy(() -> service.reorder(1L, List.of(a.publicId())))
                .isInstanceOf(InvalidCollectionOrderException.class);

        service.reorder(1L, List.of(b.publicId(), a.publicId()));
        assertThat(b.position()).isEqualTo(1);
        assertThat(a.position()).isEqualTo(2);
    }
}
