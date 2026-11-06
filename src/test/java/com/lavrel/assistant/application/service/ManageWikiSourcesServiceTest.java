package com.lavrel.assistant.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.assistant.application.port.out.GuideRepositoryPort;
import com.lavrel.assistant.application.port.out.WikiSourceRepositoryPort;
import com.lavrel.catalog.application.port.in.CheckPublishedGameUseCase;
import com.lavrel.catalog.domain.GameNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class ManageWikiSourcesServiceTest {

    private final WikiSourceRepositoryPort sources = mock(WikiSourceRepositoryPort.class);
    private final GuideRepositoryPort guides = mock(GuideRepositoryPort.class);
    private final CheckPublishedGameUseCase catalog = mock(CheckPublishedGameUseCase.class);
    private final ManageWikiSourcesService service =
            new ManageWikiSourcesService(sources, guides, catalog, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));

    @Test
    void aNewWikiForgetsTheGamesSavedGuides() {
        service.set(1L, "https://hades.fandom.com/api.php", "CC BY-SA 3.0");

        verify(sources).save(any());
        verify(guides).deleteByGame(1L);
    }

    @Test
    void onlyPublishedGames() {
        doThrow(new GameNotFoundException(9L)).when(catalog).requirePublished(9L);

        assertThatThrownBy(() -> service.set(9L, "https://x.fandom.com/api.php", "CC BY-SA 3.0"))
                .isInstanceOf(GameNotFoundException.class);
        verify(sources, never()).save(any());
    }

    @Test
    void removingAWikiForgetsItsGuides() {
        when(sources.delete(1L)).thenReturn(true);

        service.remove(1L);

        verify(guides).deleteByGame(1L);
    }

    @Test
    void removingAMissingWikiChangesNothing() {
        when(sources.delete(2L)).thenReturn(false);

        service.remove(2L);

        verify(guides, never()).deleteByGame(anyLong());
    }
}
