package com.lavrel.assistant.adapter.out.reference;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.assistant.domain.WikiSiteInfo;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MediaWikiClientTest {

    @Test
    void readsWhatTheWikiSaysAboutItself() {
        Map<String, Object> root = Map.of("query", Map.of(
                "general", Map.of("sitename", "Baldur's Gate 3 Wiki", "server", "//bg3.wiki", "articlepath", "/wiki/$1"),
                "rightsinfo", Map.of("url", "https://creativecommons.org/licenses/by-sa/4.0/",
                        "text", "Creative Commons Attribution-ShareAlike"),
                "statistics", Map.of("articles", 9100)));

        assertThat(MediaWikiClient.parseSiteInfo(root)).contains(new WikiSiteInfo("Baldur's Gate 3 Wiki", "//bg3.wiki",
                "https://creativecommons.org/licenses/by-sa/4.0/", "Creative Commons Attribution-ShareAlike",
                "https://bg3.wiki/wiki/$1", 9100));
    }

    @Test
    void anythingElseIsNoAnswer() {
        assertThat(MediaWikiClient.parseSiteInfo(Map.of())).isEmpty();
        assertThat(MediaWikiClient.parseSiteInfo(Map.of("query", Map.of("pages", Map.of())))).isEmpty();
    }
}
