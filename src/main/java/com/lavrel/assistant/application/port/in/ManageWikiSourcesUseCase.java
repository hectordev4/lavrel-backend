package com.lavrel.assistant.application.port.in;

import com.lavrel.assistant.domain.InvalidWikiSourceException;
import com.lavrel.assistant.domain.WikiSource;
import com.lavrel.catalog.domain.GameNotFoundException;
import java.util.List;

/**
 * Admin: which wiki the assistant may use as reference for a game. The admin is responsible for checking that the
 * wiki's license allows reuse (most Fandom wikis: CC BY-SA). Changing or removing a game's wiki forgets its saved
 * guides, so they are written again from the new reference.
 */
public interface ManageWikiSourcesUseCase {

    /**
     * @throws GameNotFoundException      if the game is not published
     * @throws InvalidWikiSourceException if the address or license is not acceptable
     */
    WikiSource set(long gameId, String apiUrl, String license);

    List<WikiSource> list();

    void remove(long gameId);

    /** @return how many saved guides were forgotten */
    int forgetGuides(long gameId);
}
