package com.lavrel.assistant.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchTermsTest {

    private static final String QUESTION = "¿Dónde consigo el anillo de la serpiente de oro?";

    @Test
    void keepsTheModelsTermsOnOneCleanLine() {
        assertThat(SearchTerms.clean("Covetous Gold Serpent Ring location", QUESTION))
                .isEqualTo("Covetous Gold Serpent Ring location");
        assertThat(SearchTerms.clean("Search terms: \"Covetous Gold Serpent Ring\".\nThis ring...", QUESTION))
                .isEqualTo("Covetous Gold Serpent Ring");
        assertThat(SearchTerms.clean("\n  **Vordt of the Boreal Valley**  ", QUESTION))
                .isEqualTo("Vordt of the Boreal Valley");
    }

    @Test
    void anythingUnusableFallsBackToTheQuestion() {
        assertThat(SearchTerms.clean(null, QUESTION)).isEqualTo(QUESTION);
        assertThat(SearchTerms.clean("  \n \"\" ", QUESTION)).isEqualTo(QUESTION);
        assertThat(SearchTerms.clean("12345", QUESTION)).isEqualTo(QUESTION);
    }

    @Test
    void longAnswersAreCut() {
        assertThat(SearchTerms.clean("word ".repeat(40), QUESTION)).hasSizeLessThanOrEqualTo(SearchTerms.MAX_LENGTH);
    }

    @Test
    void theFocusWordSkipsGenericWords() {
        assertThat(SearchTerms.focusWord("Covetous Gold Serpent Ring location")).isEqualTo("Covetous");
        assertThat(SearchTerms.focusWord("how to beat Malenia")).isEqualTo("Malenia");
        assertThat(SearchTerms.focusWord("all boss list")).isNull();
        assertThat(SearchTerms.focusWord(null)).isNull();
    }
}
