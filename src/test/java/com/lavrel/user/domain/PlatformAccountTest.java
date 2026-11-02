package com.lavrel.user.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lavrel.shared.domain.Platform;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformAccountTest {

    private static final Instant NOW = Instant.parse("2026-11-02T10:00:00Z");

    @Test
    void linkedAccountIsVerifiedAndNeverSynced() {
        PlatformAccount account = PlatformAccount.linkVerified(1L, Platform.STEAM, "7656", "Nick", NOW);
        assertThat(account.verified()).isTrue();
        assertThat(account.verifiedAt()).isEqualTo(NOW);
        assertThat(account.lastSyncedAt()).isNull();
        assertThat(account.belongsTo(1L)).isTrue();
        assertThat(account.belongsTo(2L)).isFalse();
    }

    @Test
    void markSyncedRecordsTheTime() {
        PlatformAccount account = PlatformAccount.linkVerified(1L, Platform.STEAM, "7656", null, NOW);
        Instant later = NOW.plusSeconds(60);
        account.markSynced(later);
        assertThat(account.lastSyncedAt()).isEqualTo(later);
    }

    @Test
    void verifiedNeedsAVerificationTime() {
        assertThatThrownBy(() -> PlatformAccount.restore(1L, 1L, Platform.STEAM, "7656", null, true, null, NOW, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void identityIsPlatformAndExternalId() {
        PlatformAccount a = PlatformAccount.linkVerified(1L, Platform.STEAM, "7656", null, NOW);
        PlatformAccount b = PlatformAccount.restore(5L, 2L, Platform.STEAM, "7656", "x", true, NOW, NOW, NOW);
        PlatformAccount xbox = PlatformAccount.linkVerified(1L, Platform.XBOX, "7656", null, NOW);
        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b).isNotEqualTo(xbox);
    }
}
