package com.lavrel.assistant.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class WikiTextTest {

    @Test
    void markupBecomesPlainText() {
        String wikitext = """
                {{Infobox achievement|name=Escape|{{nested|x}}}}
                == How to unlock ==
                Defeat [[Hades (character)|Hades]] in [[Elysium]].<ref>Patch notes</ref> It is '''not''' missable.
                [[File:Escape.png|thumb]]
                <!-- editor note -->
                {| class="wikitable"
                |-
                | Escape || Escape the Underworld
                |}
                """;
        String text = WikiText.clean(wikitext);
        assertThat(text).contains("How to unlock:").contains("Defeat Hades in Elysium.").contains("It is not missable.")
                .contains("Escape | Escape the Underworld")
                .doesNotContain("{{").doesNotContain("[[").doesNotContain("<ref").doesNotContain("editor note")
                .doesNotContain("File:");
    }

    @Test
    void cutsAroundTheAchievement() {
        String text = "a".repeat(5000) + " The Escape achievement needs ten runs. " + "b".repeat(5000);
        String excerpt = WikiText.around(text, "escape achievement", 1000);
        assertThat(excerpt).contains("The Escape achievement needs ten runs.").hasSizeLessThanOrEqualTo(1002);
    }

    @Test
    void shortTextIsKeptWhole() {
        assertThat(WikiText.around("short", "x", 1000)).isEqualTo("short");
    }
}
