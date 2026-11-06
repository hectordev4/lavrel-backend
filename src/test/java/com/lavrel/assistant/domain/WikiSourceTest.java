package com.lavrel.assistant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class WikiSourceTest {

    private static WikiSource source(String apiUrl) {
        return new WikiSource(1L, apiUrl, "CC BY-SA 3.0", Instant.EPOCH);
    }

    @Test
    void acceptsPublicMediaWikiApis() {
        assertThat(source("https://Hades.fandom.com/api.php").apiUrl()).isEqualTo("https://hades.fandom.com/api.php");
        assertThat(source("https://terraria.wiki.gg/api.php").apiUrl()).isEqualTo("https://terraria.wiki.gg/api.php");
    }

    @Test
    void refusesAnythingThatCouldReachInternalMachines() {
        for (String bad : new String[] {"http://hades.fandom.com/api.php", "https://localhost/api.php",
                "https://127.0.0.1/api.php", "https://10.0.0.5/api.php", "https://wiki.local/api.php",
                "https://hades.fandom.com:8443/api.php", "https://user@hades.fandom.com/api.php",
                "https://hades.fandom.com/wiki/Main_Page", "https://hades.fandom.com/api.php?x=1"}) {
            assertThatThrownBy(() -> source(bad)).as(bad).isInstanceOf(InvalidWikiSourceException.class);
        }
    }

    @Test
    void buildsPageAddresses() {
        assertThat(source("https://hades.fandom.com/api.php").pageUrl("Is There No Escape?"))
                .isEqualTo("https://hades.fandom.com/wiki/Is_There_No_Escape%3F");
    }

    @Test
    void aLicenseIsRequired() {
        assertThatThrownBy(() -> new WikiSource(1L, "https://hades.fandom.com/api.php", " ", Instant.EPOCH))
                .isInstanceOf(InvalidWikiSourceException.class);
    }
}
