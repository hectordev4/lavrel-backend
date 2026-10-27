package com.lavrel.catalog.application.port.in;

/**
 * Gives details to games that were never enriched (games imported before details existed): checks them against
 * the content policy, publishes the safe ones, and removes adult ones and games Steam no longer knows.
 */
public interface EnrichGamesUseCase {

    EnrichResult enrichPendingGames(int maxGames);

    /** remaining: games still waiting for details; call again until it is 0. */
    record EnrichResult(int checked, int removed, int remaining, boolean stoppedEarly) {
    }
}
