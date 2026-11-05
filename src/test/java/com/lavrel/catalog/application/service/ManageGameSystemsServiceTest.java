package com.lavrel.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.catalog.application.port.out.GameRepositoryPort;
import com.lavrel.catalog.application.port.out.GameSystemRepositoryPort;
import com.lavrel.catalog.application.port.out.GameSystemRepositoryPort.Source;
import com.lavrel.catalog.domain.Game;
import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.catalog.domain.GameSystem;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ManageGameSystemsServiceTest {

    private final GameRepositoryPort games = mock(GameRepositoryPort.class);
    private final GameSystemRepositoryPort systems = mock(GameSystemRepositoryPort.class);
    private final ManageGameSystemsService service = new ManageGameSystemsService(games, systems);

    @Test
    void adminEntriesAreMarkedManual() {
        when(games.findById(7L)).thenReturn(Optional.of(Game.restore(7L, "Game", null, "", null, Instant.EPOCH)));

        service.add(7L, GameSystem.PLAYSTATION);

        verify(systems).add(7L, GameSystem.PLAYSTATION, Source.MANUAL);
    }

    @Test
    void unknownGamesAreNotFound() {
        when(games.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.add(9L, GameSystem.XBOX)).isInstanceOf(GameNotFoundException.class);
        verify(systems, never()).add(anyLong(), any(), any());
    }
}
