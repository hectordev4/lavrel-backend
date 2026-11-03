package com.lavrel.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class UsernamesTest {

    @Test
    void acceptsFreeDisplayNamesWithAccentsSpacesAndOtherScripts() {
        assertThat(Usernames.validate("Héctor K.")).isEqualTo("Héctor K.");
        assertThat(Usernames.validate("ユウキ")).isEqualTo("ユウキ");
        assertThat(Usernames.validate("[CLAN] Night-Owl!")).isEqualTo("[CLAN] Night-Owl!");
    }

    @Test
    void trimsAndCollapsesSpaces() {
        assertThat(Usernames.validate("  Big    Boss  ")).isEqualTo("Big Boss");
    }

    @Test
    void enforcesLength() {
        assertThatThrownBy(() -> Usernames.validate("a")).isInstanceOf(InvalidUsernameException.class);
        assertThatThrownBy(() -> Usernames.validate("x".repeat(33))).isInstanceOf(InvalidUsernameException.class);
        assertThat(Usernames.validate("x".repeat(32))).hasSize(32);
    }

    @Test
    void rejectsControlCharactersAndOddSymbols() {
        assertThatThrownBy(() -> Usernames.validate("bad\u0000name")).isInstanceOf(InvalidUsernameException.class);
        assertThatThrownBy(() -> Usernames.validate("<script>")).isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void rejectsNamesThatPoseAsStaffEvenWhenDisguised() {
        assertThatThrownBy(() -> Usernames.validate("Admin")).isInstanceOf(InvalidUsernameException.class);
        assertThatThrownBy(() -> Usernames.validate("a.d.m.i.n")).isInstanceOf(InvalidUsernameException.class);
        assertThatThrownBy(() -> Usernames.validate("Lavrel Support")).isInstanceOf(InvalidUsernameException.class);
    }

    @Test
    void everyFriendlyNameCombinationIsAValidUsername() {
        for (String adjective : FriendlyNames.ADJECTIVES) {
            for (String noun : FriendlyNames.NOUNS) {
                String name = adjective + noun;
                assertThat(Usernames.validate(name)).isEqualTo(name);
            }
        }
    }

    @Test
    void friendlyNamesAreAdjectivePlusNoun() {
        java.util.random.RandomGenerator first = new java.util.Random(7);
        java.util.random.RandomGenerator second = new java.util.Random(7);
        String name = FriendlyNames.compose(first);
        assertThat(name).isEqualTo(FriendlyNames.compose(second));
        assertThat(FriendlyNames.ADJECTIVES.stream().anyMatch(name::startsWith)).isTrue();
        assertThat(FriendlyNames.NOUNS.stream().anyMatch(name::endsWith)).isTrue();
    }

    @Test
    void publicIdsAlwaysHaveSixteenDigits() {
        var random = new java.security.SecureRandom();
        for (int i = 0; i < 1000; i++) {
            long id = PublicIds.draw(random);
            assertThat(String.valueOf(id)).hasSize(16);
            assertThat(PublicIds.isValid(id)).isTrue();
        }
    }
}
