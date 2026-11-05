package com.lavrel.profile.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProfileSettingsTest {

    @Test
    void everythingIsPrivateByDefault() {
        ProfileSettings s = ProfileSettings.defaults(1L);
        assertThat(s.profilePublic()).isFalse();
        assertThat(s.showCountry()).isFalse();
        assertThat(s.showPlatforms()).isFalse();
        assertThat(s.showAvatar()).isFalse();
    }

    @Test
    void partialUpdateKeepsTheRest() {
        ProfileSettings s = ProfileSettings.defaults(1L);
        assertThat(s.update(true, null, null, null)).isTrue();
        assertThat(s.profilePublic()).isTrue();
        assertThat(s.showPlatforms()).isFalse();
        assertThat(s.update(true, null, null, null)).isFalse();
    }
}
