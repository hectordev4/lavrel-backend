package com.lavrel.assistant.application.service;

import com.lavrel.assistant.application.port.in.ManageWikiSourcesUseCase;
import com.lavrel.assistant.application.port.out.GuideRepositoryPort;
import com.lavrel.assistant.application.port.out.ReferenceWikiRepositoryPort;
import com.lavrel.assistant.application.port.out.ReferenceWikiRepositoryPort.WikiWithGames;
import com.lavrel.assistant.application.port.out.WikiClientPort;
import com.lavrel.assistant.domain.InvalidWikiSourceException;
import com.lavrel.assistant.domain.ReferenceWiki;
import com.lavrel.assistant.domain.WikiLicenses;
import com.lavrel.assistant.domain.WikiSiteInfo;
import com.lavrel.assistant.domain.WikiUnreachableException;
import com.lavrel.catalog.application.port.in.CheckPublishedGameUseCase;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The wiki is asked about itself first (a network call, so outside any transaction), then the database changes run in
 * one short transaction.
 */
@Service
public class ManageWikiSourcesService implements ManageWikiSourcesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageWikiSourcesService.class);

    private final ReferenceWikiRepositoryPort wikis;
    private final WikiClientPort wikiClient;
    private final GuideRepositoryPort guides;
    private final CheckPublishedGameUseCase catalog;
    private final TransactionTemplate tx;
    private final Clock clock;

    public ManageWikiSourcesService(ReferenceWikiRepositoryPort wikis, WikiClientPort wikiClient,
                                    GuideRepositoryPort guides, CheckPublishedGameUseCase catalog,
                                    TransactionTemplate tx, Clock clock) {
        this.wikis = wikis;
        this.wikiClient = wikiClient;
        this.guides = guides;
        this.catalog = catalog;
        this.tx = tx;
        this.clock = clock;
    }

    @Override
    public GameWiki approve(long gameId, String rawApiUrl) {
        catalog.requirePublished(gameId);
        String apiUrl = ReferenceWiki.validApiUrl(rawApiUrl);
        WikiSiteInfo info = wikiClient.siteInfo(apiUrl).orElseThrow(() -> new WikiUnreachableException(apiUrl));
        String host = apiUrl.substring("https://".length(), apiUrl.indexOf('/', "https://".length()));
        if (!info.serverHost().isEmpty() && !info.serverHost().equals(host)) {
            throw new InvalidWikiSourceException("The wiki says it is served from " + info.serverHost()
                    + ", not " + host + ": approve that address instead");
        }
        String license = WikiLicenses.allowed(info);
        Instant now = clock.instant();
        GameWiki result = tx.execute(status -> {
            ReferenceWiki wiki = wikis.save(wikis.findByApiUrl(apiUrl)
                    .map(stored -> stored.refreshedFrom(info, license))
                    .orElseGet(() -> ReferenceWiki.approved(apiUrl, info, license, now)));
            Optional<ReferenceWiki> previous = wikis.findForGame(gameId);
            int forgotten = 0;
            if (previous.isEmpty() || !previous.get().id().equals(wiki.id())) {
                wikis.link(gameId, wiki.id(), now);
                forgotten = guides.deleteByGame(gameId);
                wikis.deleteUnused();
            }
            return new GameWiki(gameId, wiki, forgotten);
        });
        log.info("Game {} uses {} ({}, {}) as reference; {} saved guides forgotten", gameId, apiUrl,
                info.siteName(), license, result.guidesForgotten());
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WikiWithGames> list() {
        return wikis.findAllWithGames();
    }

    @Override
    @Transactional
    public void remove(long gameId) {
        if (wikis.unlink(gameId)) {
            guides.deleteByGame(gameId);
            wikis.deleteUnused();
            log.info("Game {} no longer has a reference wiki", gameId);
        }
    }

    @Override
    @Transactional
    public int forgetGuides(long gameId) {
        return guides.deleteByGame(gameId);
    }
}
