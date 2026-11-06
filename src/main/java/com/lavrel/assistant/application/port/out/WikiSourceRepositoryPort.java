package com.lavrel.assistant.application.port.out;

import com.lavrel.assistant.domain.WikiSource;
import java.util.List;
import java.util.Optional;

/** assistant_wiki (migration V26): at most one approved wiki per game. */
public interface WikiSourceRepositoryPort {

    Optional<WikiSource> find(long gameId);

    List<WikiSource> findAll();

    /** Inserts or replaces the game's wiki. */
    void save(WikiSource source);

    boolean delete(long gameId);
}
