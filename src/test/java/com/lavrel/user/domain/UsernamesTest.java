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
        for (String name : new String[] {"Admin", "a.d.m.i.n", "Lavrel Support", "TheLavrelTeam", "L4vrel",
                "Lavrel_Official", "Admin 2", "Admin123", "4dm1n", "\u0430dmin", "\u00c1dmin", "Head Admin",
                "Official Steve", "Moderator Kai", "M0D", "S.U.P.P.O.R.T", "Staff", "Soporte", "xX_Admin_Xx"}) {
            assertThatThrownBy(() -> Usernames.validate(name)).as(name)
                    .isInstanceOf(UsernameReservedException.class);
        }
    }

    @Test
    void allowsOrdinaryNamesThatOnlyLookCloseToAReservedWord() {
        for (String name : new String[] {"Support Main", "Badminton Pro", "Modern Gamer", "Team 7",
                "Developer Diaries", "Laurel", "Steamroller", "Admiral Kim", "Mr Moderate"}) {
            assertThat(Usernames.validate(name)).as(name).isEqualTo(name);
        }
    }

    @Test
    void staffMayUseReservedNames() {
        assertThat(Usernames.validate("Lavrel Support", true)).isEqualTo("Lavrel Support");
        assertThatThrownBy(() -> Usernames.validate("x", true)).isInstanceOf(InvalidUsernameException.class);
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
