package com.lavrel.assistant.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.lavrel.assistant.application.port.out.ReferenceWikiRepositoryPort.WikiWithGames;
import com.lavrel.assistant.domain.ReferenceWiki;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * reference_wiki and game_wiki against a real PostgreSQL (schema from the Flyway migrations). Needs Docker: without it
 * the class is skipped (GitHub CI has Docker, so it always runs there). Each test rolls back.
 */
@DataJpaTest
@Import(ReferenceWikiJdbcAdapter.class)
@Testcontainers(disabledWithoutDocker = true)
class ReferenceWikiJdbcAdapterTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final Instant NOW = Instant.parse("2026-11-23T10:00:00Z");

    @Autowired
    private ReferenceWikiJdbcAdapter wikis;

    @Autowired
    private JdbcTemplate jdbc;

    private long ds1;
    private long ds3;
    private long hades;

    @BeforeEach
    void threeGames() {
        ds1 = game("Dark Souls", "dark-souls-test");
        ds3 = game("Dark Souls III", "dark-souls-iii-test");
        hades = game("Hades", "hades-test");
    }

    private long game(String title, String slug) {
        return jdbc.queryForObject("INSERT INTO game (title, slug) VALUES (?, ?) RETURNING id", Long.class, title, slug);
    }

    private static ReferenceWiki darkSouls() {
        return new ReferenceWiki(null, "https://darksouls.fandom.com/api.php", "Dark Souls Wiki", "CC BY-SA 3.0",
                "https://creativecommons.org/licenses/by-sa/3.0/", "https://darksouls.fandom.com/wiki/$1", NOW);
    }

    @Test
    void oneWikiServesSeveralGames() {
        ReferenceWiki wiki = wikis.save(darkSouls());
        wikis.link(ds1, wiki.id(), NOW);
        wikis.link(ds3, wiki.id(), NOW);

        assertThat(wikis.findForGame(ds1)).contains(wiki);
        assertThat(wikis.findForGame(ds3)).contains(wiki);
        assertThat(wikis.findForGame(hades)).isEmpty();
        assertThat(wikis.findAllWithGames()).singleElement()
                .isEqualTo(new WikiWithGames(wiki, List.of(ds1, ds3)));
    }

    @Test
    void theSameAddressIsStoredOnce() {
        ReferenceWiki first = wikis.save(darkSouls());
        ReferenceWiki again = wikis.save(darkSouls());

        assertThat(again.id()).isEqualTo(first.id());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM reference_wiki", Integer.class)).isEqualTo(1);
    }

    @Test
    void aStoredWikiCanBeRefreshed() {
        ReferenceWiki stored = wikis.save(darkSouls());

        wikis.save(new ReferenceWiki(stored.id(), stored.apiUrl(), "Dark Souls Wiki (new name)", "CC BY-SA 4.0",
                null, null, stored.addedAt()));

        assertThat(wikis.findByApiUrl(stored.apiUrl())).get().satisfies(w -> {
            assertThat(w.siteName()).isEqualTo("Dark Souls Wiki (new name)");
            assertThat(w.license()).isEqualTo("CC BY-SA 4.0");
        });
    }

    @Test
    void linkingAgainReplacesTheGamesWikiAndUnusedWikisCanBeRemoved() {
        ReferenceWiki ds = wikis.save(darkSouls());
        ReferenceWiki other = wikis.save(new ReferenceWiki(null, "https://hades.fandom.com/api.php", "Hades Wiki",
                "CC BY-SA 3.0", null, null, NOW));
        wikis.link(hades, ds.id(), NOW);

        wikis.link(hades, other.id(), NOW);

        assertThat(wikis.findForGame(hades)).contains(other);
        assertThat(wikis.deleteUnused()).isEqualTo(1);
        assertThat(wikis.findByApiUrl(ds.apiUrl())).isEmpty();
    }

    @Test
    void unlinkingSaysWhetherTheGameHadAWiki() {
        ReferenceWiki wiki = wikis.save(darkSouls());
        wikis.link(ds1, wiki.id(), NOW);

        assertThat(wikis.unlink(ds1)).isTrue();
        assertThat(wikis.unlink(ds1)).isFalse();
        assertThat(wikis.findForGame(ds1)).isEmpty();
    }
}
