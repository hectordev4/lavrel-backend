package com.lavrel.assistant.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class WikiPagesTest {

    private static final List<String> HITS = List.of("Firelink Shrine", "Firelink Shrine (Dark Souls II)",
            "Firelink Shrine (Dark Souls III)", "Andre of Astora");

    @Test
    void pagesAboutThisGameFirstThenGeneralThenOtherGames() {
        assertThat(WikiPages.preferFor("DARK SOULS™ III", HITS, 4)).containsExactly(
                "Firelink Shrine (Dark Souls III)", "Firelink Shrine", "Andre of Astora",
                "Firelink Shrine (Dark Souls II)");
    }

    @Test
    void keepsOnlyTheBestOnes() {
        assertThat(WikiPages.preferFor("Dark Souls III", HITS, 2))
                .containsExactly("Firelink Shrine (Dark Souls III)", "Firelink Shrine");
        assertThat(WikiPages.preferFor("Dark Souls III", List.of("One"), 2)).containsExactly("One");
    }

    @Test
    void aWikiForOneGameKeepsItsOwnOrder() {
        assertThat(WikiPages.preferFor("Hades", List.of("Zagreus", "Stygian Blade"), 2))
                .containsExactly("Zagreus", "Stygian Blade");
    }

    @Test
    void namesAreComparedWithoutCapitalsSymbolsOrPunctuation() {
        assertThat(WikiPages.normalized("Monster Hunter: World")).isEqualTo("monster hunter world");
        assertThat(WikiPages.rank("monster hunter world", "Rathalos (World)")).isZero();
        assertThat(WikiPages.rank("monster hunter world", "Rathalos (Rise)")).isEqualTo(2);
        assertThat(WikiPages.rank("monster hunter world", "Rathalos")).isEqualTo(1);
    }
}
