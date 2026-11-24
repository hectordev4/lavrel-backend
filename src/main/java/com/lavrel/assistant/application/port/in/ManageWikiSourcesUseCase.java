package com.lavrel.assistant.application.port.in;

import com.lavrel.assistant.application.port.out.ReferenceWikiRepositoryPort.WikiWithGames;
import com.lavrel.assistant.domain.InvalidWikiSourceException;
import com.lavrel.assistant.domain.ReferenceWiki;
import com.lavrel.assistant.domain.WikiLicenseNotAllowedException;
import com.lavrel.assistant.domain.WikiUnreachableException;
import com.lavrel.catalog.domain.GameNotFoundException;
import java.util.List;

/**
 * Admin: which wiki the assistant may read for a game. The admin only gives the wiki's API address; the backend asks
 * the wiki itself for its name and license, and only accepts CC BY-SA (Fandom, wiki.gg and most game wikis). A wiki is
 * stored once and can serve several games (a franchise wiki). Giving a game a different wiki, or removing it, forgets
 * that game's saved guides, so they are written again from the new reference.
 */
public interface ManageWikiSourcesUseCase {

    /**
     * Approves the wiki for the game (adds it if new, refreshes its name and license if already stored) and links it.
     *
     * @throws GameNotFoundException          if the game is not published
     * @throws InvalidWikiSourceException     if the address is not a public https api.php, or the wiki says it is
     *                                        served from another host
     * @throws WikiUnreachableException       if the wiki does not answer as a MediaWiki
     * @throws WikiLicenseNotAllowedException if its license is not CC BY-SA (or is non-commercial)
     */
    GameWiki approve(long gameId, String apiUrl);

    List<WikiWithGames> list();

    /** Unlinks the game's wiki (and removes the wiki if no other game uses it). */
    void remove(long gameId);

    /** @return how many saved guides were forgotten */
    int forgetGuides(long gameId);

    /** @param guidesForgotten saved guides of the game forgotten because its wiki changed (0 if it was the same one) */
    record GameWiki(long gameId, ReferenceWiki wiki, int guidesForgotten) {
    }
}
