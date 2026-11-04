package com.lavrel.library.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CollectionNamesTest {

    @Test
    void trimsAndCollapsesSpaces() {
        assertThat(CollectionNames.validate("  Cozy    games ")).isEqualTo("Cozy games");
    }

    @Test
    void acceptsAnyAlphabetAndSymbols() {
        assertThat(CollectionNames.validate("Platinum hunt 🏆 (2026)")).isEqualTo("Platinum hunt 🏆 (2026)");
    }

    @Test
    void rejectsEmptyTooLongAndControlCharacters() {
        assertThatThrownBy(() -> CollectionNames.validate("   ")).isInstanceOf(InvalidCollectionNameException.class);
        assertThatThrownBy(() -> CollectionNames.validate("x".repeat(41))).isInstanceOf(InvalidCollectionNameException.class);
        assertThatThrownBy(() -> CollectionNames.validate("bad\u0000name")).isInstanceOf(InvalidCollectionNameException.class);
        assertThat(CollectionNames.validate("x".repeat(40))).hasSize(40);
    }
}
