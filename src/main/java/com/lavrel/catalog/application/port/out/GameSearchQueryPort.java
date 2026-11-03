package com.lavrel.catalog.application.port.out;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GamePage;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameSearchCriteria;

/** Read-only search over published games (Postgres trigram + unaccent, migration V16). */
public interface GameSearchQueryPort {

    GamePage search(GameSearchCriteria criteria);
}
