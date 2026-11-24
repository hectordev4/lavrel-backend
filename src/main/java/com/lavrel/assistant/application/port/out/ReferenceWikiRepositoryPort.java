package com.lavrel.assistant.application.port.out;

import com.lavrel.assistant.domain.ReferenceWiki;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * reference_wiki and game_wiki (migration V35): each wiki is stored once, and each game is linked to at most one wiki.
 * Several games can share a wiki (a franchise wiki).
 */
public interface ReferenceWikiRepositoryPort {

    Optional<ReferenceWiki> findForGame(long gameId);

    Optional<ReferenceWiki> findByApiUrl(String apiUrl);

    /** Inserts a new wiki (id null) or updates a stored one's name and license. @return it, with its id */
    ReferenceWiki save(ReferenceWiki wiki);

    /** Links the game to the wiki, replacing any wiki it had. */
    void link(long gameId, long wikiId, Instant at);

    /** @return true if the game had a wiki */
    boolean unlink(long gameId);

    /** Removes wikis no game uses any more. @return how many */
    int deleteUnused();

    /** Every wiki with the games that use it, by wiki name. */
    List<WikiWithGames> findAllWithGames();

    record WikiWithGames(ReferenceWiki wiki, List<Long> gameIds) {
    }
}
