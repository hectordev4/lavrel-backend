package com.lavrel.assistant.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ReferenceWikiTest {

    private static ReferenceWiki wiki(String apiUrl, String articleUrl) {
        return new ReferenceWiki(1L, apiUrl, "Wiki", "CC BY-SA 3.0", null, articleUrl, Instant.EPOCH);
    }

    @Test
    void acceptsPublicMediaWikiApis() {
        assertThat(ReferenceWiki.validApiUrl("https://Hades.fandom.com/api.php"))
                .isEqualTo("https://hades.fandom.com/api.php");
        assertThat(ReferenceWiki.validApiUrl("https://terraria.wiki.gg/api.php"))
                .isEqualTo("https://terraria.wiki.gg/api.php");
        assertThat(ReferenceWiki.validApiUrl("https://bg3.wiki/w/api.php")).isEqualTo("https://bg3.wiki/w/api.php");
    }

    @Test
    void refusesAnythingThatCouldReachInternalMachines() {
        for (String bad : new String[] {"http://hades.fandom.com/api.php", "https://localhost/api.php",
                "https://127.0.0.1/api.php", "https://10.0.0.5/api.php", "https://wiki.local/api.php",
                "https://hades.fandom.com:8443/api.php", "https://user@hades.fandom.com/api.php",
                "https://hades.fandom.com/wiki/Main_Page", "https://hades.fandom.com/api.php?x=1", " "}) {
            assertThatThrownBy(() -> ReferenceWiki.validApiUrl(bad)).as(bad)
                    .isInstanceOf(InvalidWikiSourceException.class);
        }
    }

    @Test
    void pagesLiveWhereTheWikiSaysOrNextToApiPhp() {
        assertThat(wiki("https://hades.fandom.com/api.php", null).pageUrl("Is There No Escape?"))
                .isEqualTo("https://hades.fandom.com/wiki/Is_There_No_Escape%3F");
        assertThat(wiki("https://bg3.wiki/w/api.php", "https://bg3.wiki/wiki/$1").pageUrl("Shadowheart"))
                .isEqualTo("https://bg3.wiki/wiki/Shadowheart");
    }

    @Test
    void aLicenseIsRequired() {
        assertThatThrownBy(() -> new ReferenceWiki(null, "https://hades.fandom.com/api.php", "Hades Wiki", " ", null,
                null, Instant.EPOCH)).isInstanceOf(InvalidWikiSourceException.class);
    }

    @Test
    void approvedAndRefreshedFromWhatTheWikiSays() {
        WikiSiteInfo info = new WikiSiteInfo("Dark Souls Wiki", "https://darksouls.fandom.com",
                "https://creativecommons.org/licenses/by-sa/3.0/", "CC-BY-SA",
                "https://darksouls.fandom.com/wiki/$1", 4200);
        ReferenceWiki wiki = ReferenceWiki.approved("https://darksouls.fandom.com/api.php", info, "CC BY-SA 3.0",
                Instant.EPOCH);

        assertThat(wiki.siteName()).isEqualTo("Dark Souls Wiki");
        assertThat(wiki.licenseUrl()).isEqualTo("https://creativecommons.org/licenses/by-sa/3.0/");
        assertThat(wiki.withId(9L).refreshedFrom(info, "CC BY-SA 3.0").id()).isEqualTo(9L);
    }
}
