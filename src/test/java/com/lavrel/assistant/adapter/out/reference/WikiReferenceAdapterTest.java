package com.lavrel.assistant.adapter.out.reference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.assistant.application.port.out.WikiClientPort;
import com.lavrel.assistant.application.port.out.ReferenceWikiRepositoryPort;
import com.lavrel.assistant.domain.AchievementContext;
import com.lavrel.assistant.domain.Excerpt;
import com.lavrel.assistant.domain.ReferenceWiki;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class WikiReferenceAdapterTest {

    private static final AchievementContext A = new AchievementContext(1L, "Hades", 10L, "Escape", "", false);

    private final ReferenceWikiRepositoryPort sources = mock(ReferenceWikiRepositoryPort.class);
    private final WikiClientPort wiki = mock(WikiClientPort.class);
    private final WikiReferenceAdapter adapter = new WikiReferenceAdapter(sources, wiki);

    @Test
    void gamesWithoutAnApprovedWikiGetNoReference() {
        when(sources.findForGame(1L)).thenReturn(Optional.empty());

        assertThat(adapter.find(A)).isEmpty();
        verify(wiki, never()).search(anyString(), anyString(), anyInt());
    }

    @Test
    void excerptsCarryTheirPageAndLicense() {
        String api = "https://hades.fandom.com/api.php";
        when(sources.findForGame(1L)).thenReturn(Optional.of(wiki(api)));
        when(wiki.search(api, "Escape", WikiReferenceAdapter.PAGES)).thenReturn(List.of("Achievements"));
        when(wiki.wikitext(api, "Achievements")).thenReturn(Optional.of("'''Escape''': beat [[Hades]]."));

        List<Excerpt> excerpts = adapter.find(A);

        assertThat(excerpts).singleElement().satisfies(e -> {
            assertThat(e.text()).isEqualTo("Escape: beat Hades.");
            assertThat(e.url()).isEqualTo("https://hades.fandom.com/wiki/Achievements");
            assertThat(e.license()).isEqualTo("CC BY-SA 3.0");
        });
    }

    @Test
    void aChatSearchesWithTheEnglishTermsAndCutsAroundTheirTellingWord() {
        String api = "https://darksouls.fandom.com/api.php";
        when(sources.findForGame(5L)).thenReturn(Optional.of(wiki(api)));
        when(wiki.search(api, "Covetous Gold Serpent Ring location", WikiReferenceAdapter.PAGES))
                .thenReturn(List.of("Covetous Gold Serpent Ring"));
        String page = "Intro. ".repeat(1000) + "Covetous Gold Serpent Ring is found in Irithyll Dungeon.";
        when(wiki.wikitext(api, "Covetous Gold Serpent Ring")).thenReturn(Optional.of(page));

        List<Excerpt> excerpts = adapter.findForGame(5L, "Covetous Gold Serpent Ring location");

        assertThat(excerpts).singleElement().satisfies(e -> {
            assertThat(e.text()).contains("Irithyll Dungeon");
            assertThat(e.text().length()).isLessThanOrEqualTo(WikiReferenceAdapter.CHARS_PER_PAGE + 2);
        });
        verify(wiki).search(api, "Covetous Gold Serpent Ring location", WikiReferenceAdapter.PAGES);
    }

    @Test
    void excerptsAreUpTo3000Characters() {
        assertThat(WikiReferenceAdapter.CHARS_PER_PAGE).isEqualTo(3000);
        assertThat(WikiReferenceAdapter.PAGES).isEqualTo(2);
    }

    @Test
    void knowsWhetherAGameHasAnApprovedWiki() {
        when(sources.findForGame(1L)).thenReturn(Optional.empty());
        when(sources.findForGame(5L)).thenReturn(Optional.of(
                wiki("https://darksouls.fandom.com/api.php")));

        assertThat(adapter.hasSource(1L)).isFalse();
        assertThat(adapter.hasSource(5L)).isTrue();
    }

    private static ReferenceWiki wiki(String apiUrl) {
        return new ReferenceWiki(1L, apiUrl, "Wiki", "CC BY-SA 3.0", null, null, Instant.EPOCH);
    }
}
