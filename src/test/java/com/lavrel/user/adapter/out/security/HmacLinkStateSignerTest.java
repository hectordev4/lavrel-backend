package com.lavrel.user.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.domain.InvalidIdentityProofException;
import com.lavrel.user.domain.InvalidIdentityProofException.Reason;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class HmacLinkStateSignerTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private static final Instant T = Instant.parse("2026-11-13T10:00:00Z");

    private static HmacLinkStateSigner at(Instant now) {
        return new HmacLinkStateSigner(SECRET, Duration.ofMinutes(10), Clock.fixed(now, ZoneOffset.UTC));
    }

    private static Reason reasonOf(Runnable verify) {
        try {
            verify.run();
            return null;
        } catch (InvalidIdentityProofException e) {
            return e.reason();
        }
    }

    @Test
    void aFreshStateGivesBackItsUser() {
        String state = at(T).sign(42L, Platform.STEAM);
        assertThat(at(T.plusSeconds(60)).verify(state, Platform.STEAM)).isEqualTo(42L);
    }

    @Test
    void anOldStateHasExpired() {
        String state = at(T).sign(42L, Platform.STEAM);
        assertThat(reasonOf(() -> at(T.plus(Duration.ofMinutes(11))).verify(state, Platform.STEAM)))
                .isEqualTo(Reason.LINK_EXPIRED);
    }

    @Test
    void damagedMissingOrOtherPlatformStatesAreInvalid() {
        String state = at(T).sign(42L, Platform.STEAM);
        assertThat(reasonOf(() -> at(T).verify(state + "x", Platform.STEAM))).isEqualTo(Reason.INVALID_LINK_STATE);
        assertThat(reasonOf(() -> at(T).verify(null, Platform.STEAM))).isEqualTo(Reason.INVALID_LINK_STATE);
        assertThat(reasonOf(() -> at(T).verify(state, Platform.XBOX))).isEqualTo(Reason.INVALID_LINK_STATE);
    }

    @Test
    void shortSecretsAreRefused() {
        assertThatThrownBy(() -> new HmacLinkStateSigner("short", Duration.ofMinutes(10), Clock.systemUTC()))
                .isInstanceOf(IllegalStateException.class);
    }
}
