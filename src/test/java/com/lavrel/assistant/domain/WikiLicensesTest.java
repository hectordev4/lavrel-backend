package com.lavrel.assistant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WikiLicensesTest {

    private static WikiSiteInfo license(String url, String text) {
        return new WikiSiteInfo("Wiki", "https://x.fandom.com", url, text, null, 100);
    }

    @Test
    void ccBySaIsAllowedWithItsVersion() {
        assertThat(WikiLicenses.allowed(license("https://creativecommons.org/licenses/by-sa/3.0/", "CC-BY-SA")))
                .isEqualTo("CC BY-SA 3.0");
        assertThat(WikiLicenses.allowed(license("https://creativecommons.org/licenses/by-sa/4.0/",
                "Creative Commons Attribution-ShareAlike"))).isEqualTo("CC BY-SA 4.0");
        assertThat(WikiLicenses.allowed(license(null, "Creative Commons Attribution-Share Alike")))
                .isEqualTo("CC BY-SA");
    }

    @Test
    void nonCommercialIsRefused() {
        assertThatThrownBy(() -> WikiLicenses.allowed(
                license("https://creativecommons.org/licenses/by-nc-sa/3.0/", "CC BY-NC-SA 3.0")))
                .isInstanceOf(WikiLicenseNotAllowedException.class).hasMessageContaining("BY-NC-SA");
        assertThatThrownBy(() -> WikiLicenses.allowed(
                license(null, "Creative Commons Attribution-NonCommercial-ShareAlike")))
                .isInstanceOf(WikiLicenseNotAllowedException.class);
    }

    @Test
    void anyOtherOrNoLicenseIsRefused() {
        assertThatThrownBy(() -> WikiLicenses.allowed(license(null, "All rights reserved")))
                .isInstanceOf(WikiLicenseNotAllowedException.class);
        assertThatThrownBy(() -> WikiLicenses.allowed(license(null, null)))
                .isInstanceOf(WikiLicenseNotAllowedException.class).hasMessageContaining("does not declare");
    }

    @Test
    void theServerHostIsReadWithOrWithoutScheme() {
        assertThat(new WikiSiteInfo("W", "//terraria.wiki.gg", null, null, null, 0).serverHost())
                .isEqualTo("terraria.wiki.gg");
        assertThat(new WikiSiteInfo("W", "https://Bg3.wiki/", null, null, null, 0).serverHost()).isEqualTo("bg3.wiki");
        assertThat(new WikiSiteInfo("W", null, null, null, null, 0).serverHost()).isEmpty();
    }
}
