package com.lavrel.catalog.application.port.out;

import java.util.Optional;

/**
 * game.slug (migration V29): the unique, readable name in a game's page address (/games/dark-souls-iii). The database
 * gives it when the game is created and never changes it, so shared links keep working when a title changes.
 */
public interface GameSlugPort {

    Optional<String> slugOf(long gameId);

    Optional<Long> gameIdOf(String slug);
}
