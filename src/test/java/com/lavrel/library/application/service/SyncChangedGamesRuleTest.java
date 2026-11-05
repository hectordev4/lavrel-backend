package com.lavrel.library.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SyncChangedGamesRuleTest {

    @Test
    void newGamesAreReadOnlyIfPlayedAtAll() {
        assertThat(SyncAccountService.playedSinceLastSync(null, 0)).isFalse();
        assertThat(SyncAccountService.playedSinceLastSync(null, 15)).isTrue();
    }

    @Test
    void knownGamesAreReadOnlyWhenPlaytimeWentUp() {
        assertThat(SyncAccountService.playedSinceLastSync(120, 120)).isFalse();
        assertThat(SyncAccountService.playedSinceLastSync(120, 150)).isTrue();
    }
}
