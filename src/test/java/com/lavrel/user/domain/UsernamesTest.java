package com.lavrel.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UsernamesTest {

    @Test
    void randomNamesAreValidAndDifferent() {
        String first = Usernames.random();
        String second = Usernames.random();
        assertThat(first).startsWith("player_").hasSize(15);
        assertThat(Usernames.validate(first)).isEqualTo(first);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void acceptsLettersDigitsAndUnderscoreAndKeepsTheCase() {
        assertThat(Usernames.validate("  Lavrel_42  ")).isEqualTo("Lavrel_42");
        assertThat(Usernames.validate("abc")).isEqualTo("abc");
    }

    @Test
    void rejectsBadLengthsAndCharacters() {
        for (String bad : new String[]{null, "", "   ", "ab", "a".repeat(21), "has space", "bad-dash",
                "emoji😀name", "semi;colon", "ñandú"}) {
            assertThatThrownBy(() -> Usernames.validate(bad)).isInstanceOf(InvalidUsernameException.class);
        }
    }

    @Test
    void rejectsReservedNamesInAnyCase() {
        assertThatThrownBy(() -> Usernames.validate("Admin")).isInstanceOf(InvalidUsernameException.class);
        assertThatThrownBy(() -> Usernames.validate("SUPPORT")).isInstanceOf(InvalidUsernameException.class);
    }
}
