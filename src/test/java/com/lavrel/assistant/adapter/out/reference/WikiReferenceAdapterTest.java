package com.lavrel.assistant.adapter.out.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.assistant.application.port.out.WikiClientPort;
import com.lavrel.assistant.application.port.out.WikiSourceRepositoryPort;
import com.lavrel.assistant.domain.AchievementContext;
import com.lavrel.assistant.domain.Excerpt;
import com.lavrel.assistant.domain.WikiSource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WikiReferenceAdapterTest {

    private static final AchievementContext A = new AchievementContext(1L, "Hades", 10L, "Escape", "", false);

    private final WikiSourceRepositoryPort sources = mock(WikiSourceRepositoryPort.class);
    private final WikiClientPort wiki = mock(WikiClientPort.class);
    private final WikiReferenceAdapter adapter = new WikiReferenceAdapter(sources, wiki);

    @Test
    void gamesWithoutAnApprovedWikiGetNoReference() {
        when(sources.find(1L)).thenReturn(Optional.empty());

        assertThat(adapter.find(A)).isEmpty();
        verify(wiki, never()).search(anyString(), anyString(), anyInt());
    }

    @Test
    void excerptsCarryTheirPageAndLicense() {
        String api = "https://hades.fandom.com/api.php";
        when(sources.find(1L)).thenReturn(Optional.of(new WikiSource(1L, api, "CC BY-SA 3.0", Instant.EPOCH)));
        when(wiki.search(api, "Escape", WikiReferenceAdapter.PAGES)).thenReturn(List.of("Achievements"));
        when(wiki.wikitext(api, "Achievements")).thenReturn(Optional.of("'''Escape''': beat [[Hades]]."));

        List<Excerpt> excerpts = adapter.find(A);

        assertThat(excerpts).singleElement().satisfies(e -> {
            assertThat(e.text()).isEqualTo("Escape: beat Hades.");
            assertThat(e.url()).isEqualTo("https://hades.fandom.com/wiki/Achievements");
            assertThat(e.license()).isEqualTo("CC BY-SA 3.0");
        });
    }
}
