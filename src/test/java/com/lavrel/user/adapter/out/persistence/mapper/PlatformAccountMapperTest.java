package com.lavrel.user.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.shared.domain.Platform;
import com.lavrel.user.domain.PlatformAccount;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlatformAccountMapperTest {

    @Test
    void everyFieldSurvivesTheRoundTrip() {
        Instant t = Instant.parse("2026-11-02T10:00:00Z");
        PlatformAccount account = PlatformAccount.restore(5L, 3L, Platform.STEAM, "7656", "Nick", true, t,
                t.plusSeconds(1), t.plusSeconds(2));

        PlatformAccount back = PlatformAccountMapper.toDomain(PlatformAccountMapper.toEntity(account));

        assertThat(back).usingRecursiveComparison().isEqualTo(account);
    }
}
