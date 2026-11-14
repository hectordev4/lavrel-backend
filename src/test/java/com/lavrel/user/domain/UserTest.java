package com.lavrel.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    private static User newUser() {
        return User.register(PublicIds.MIN + 1, "uid-1", "SwiftOtter", "me@mail.com", false, "photo.png", NOW);
    }

    @Test
    void registeredUserIsAPlainUserWithoutIdOrCountry() {
        User user = newUser();
        assertThat(user.id()).isNull();
        assertThat(user.role()).isEqualTo(Role.USER);
        assertThat(user.countryId()).isNull();
        assertThat(user.username()).isEqualTo("SwiftOtter");
    }

    @Test
    void publicIdMustHaveSixteenDigits() {
        assertThatThrownBy(() -> User.register(42L, "uid", "SwiftOtter", null, false, null, NOW))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void changeUsernameValidatesAndReportsChanges() {
        User user = newUser();
        assertThat(user.changeUsername("  Héctor K. ")).isTrue();
        assertThat(user.username()).isEqualTo("Héctor K.");
        assertThat(user.changeUsername("Héctor K.")).isFalse();
    }

    @Test
    void invalidUsernameLeavesTheOldOne() {
        User user = newUser();
        assertThatThrownBy(() -> user.changeUsername("admin")).isInstanceOf(InvalidUsernameException.class);
        assertThat(user.username()).isEqualTo("SwiftOtter");
    }

    @Test
    void syncSignInOnlyReportsRealChanges() {
        User user = newUser();
        assertThat(user.syncSignIn("me@mail.com", false, "photo.png")).isFalse();
        assertThat(user.syncSignIn("me@mail.com", true, "photo.png")).isTrue();
        assertThat(user.emailVerified()).isTrue();
    }

    @Test
    void changeRoleReportsChanges() {
        User user = newUser();
        assertThat(user.changeRole(Role.USER)).isFalse();
        assertThat(user.changeRole(Role.ADMIN)).isTrue();
        assertThat(user.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void changeCountrySetsAndClears() {
        User user = newUser();
        user.changeCountry(3);
        assertThat(user.countryId()).isEqualTo(3);
        user.changeCountry(null);
        assertThat(user.countryId()).isNull();
    }

    @Test
    void sameFirebaseAccountMeansSameUser() {
        User a = newUser();
        User b = User.restore(9L, PublicIds.MIN + 2, "uid-1", "Other", null, true, null, null, Role.ADMIN, NOW, null);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
    }

    @Test
    void toStringHidesPrivateData() {
        String text = newUser().toString();
        assertThat(text).doesNotContain("me@mail.com", "photo.png", "uid-1").contains("SwiftOtter");
    }

    @Test
    void aNewUserHasNotDoneTheFirstSetup() {
        User user = newUser();
        assertThat(user.setupCompleted()).isFalse();
        assertThat(user.setupCompletedAt()).isNull();
    }

    @Test
    void completeSetupKeepsTheFirstTime() {
        User user = newUser();
        Instant later = NOW.plusSeconds(60);

        assertThat(user.completeSetup(NOW)).isTrue();
        assertThat(user.completeSetup(later)).isFalse();

        assertThat(user.setupCompleted()).isTrue();
        assertThat(user.setupCompletedAt()).isEqualTo(NOW);
    }
}
