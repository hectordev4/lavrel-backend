package com.lavrel.assistant.application.service;

import com.lavrel.assistant.application.port.in.ManageWikiSourcesUseCase;
import com.lavrel.assistant.application.port.out.GuideRepositoryPort;
import com.lavrel.assistant.application.port.out.WikiSourceRepositoryPort;
import com.lavrel.assistant.domain.WikiSource;
import com.lavrel.catalog.application.port.in.CheckPublishedGameUseCase;
import java.time.Clock;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ManageWikiSourcesService implements ManageWikiSourcesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ManageWikiSourcesService.class);

    private final WikiSourceRepositoryPort sources;
    private final GuideRepositoryPort guides;
    private final CheckPublishedGameUseCase catalog;
    private final Clock clock;

    public ManageWikiSourcesService(WikiSourceRepositoryPort sources, GuideRepositoryPort guides,
                                    CheckPublishedGameUseCase catalog, Clock clock) {
        this.sources = sources;
        this.guides = guides;
        this.catalog = catalog;
        this.clock = clock;
    }

    @Override
    @Transactional
    public WikiSource set(long gameId, String apiUrl, String license) {
        catalog.requirePublished(gameId);
        WikiSource source = new WikiSource(gameId, apiUrl, license, clock.instant());
        sources.save(source);
        int forgotten = guides.deleteByGame(gameId);
        log.info("Game {} now uses {} ({}) as reference; {} saved guides forgotten", gameId, source.apiUrl(),
                source.license(), forgotten);
        return source;
    }

    @Override
    @Transactional(readOnly = true)
    public List<WikiSource> list() {
        return sources.findAll();
    }

    @Override
    @Transactional
    public void remove(long gameId) {
        if (sources.delete(gameId)) {
            guides.deleteByGame(gameId);
            log.info("Game {} no longer has a reference wiki", gameId);
        }
    }

    @Override
    @Transactional
    public int forgetGuides(long gameId) {
        return guides.deleteByGame(gameId);
    }
}
