package com.lavrel.catalog.application.port.in;

import java.util.List;

/**
 * Content-policy exceptions: games an admin lets into the catalog although the policy would reject them
 * (Marvel Rivals has the Hentai and Sexual Content user tags with enough weight to be rejected). An exempt
 * game skips the content policy on import, enrich and refresh; it must still be a real game with
 * achievements. No extra table: the exception is a flag on the game.
 */
public interface ManageContentExemptionsUseCase {

    /**
     * Exempts the given Steam apps. Games already in the catalog are just flagged; the others are imported
     * without the content policy (their remembered rejection is forgotten first) and then flagged.
     */
    List<ExemptionResult> exempt(List<String> steamAppIds);

    /**
     * Removes the exception. The game stays for now; the next refresh judges it by the policy again.
     * @return false if the app is not in the catalog
     */
    boolean removeExemption(String steamAppId);

    List<ExemptGame> listExemptions();

    enum Status {
        /** Was already in the catalog; now exempt. */
        EXEMPTED,
        /** Imported now and exempt. */
        IMPORTED,
        /** Cannot be a catalog game even without the policy (not a game, no achievements...); see reason. */
        NOT_IMPORTABLE,
        /** Steam could not be reached; try again. */
        DEFERRED
    }

    /** gameId is set for EXEMPTED and IMPORTED; reason for NOT_IMPORTABLE and DEFERRED. */
    record ExemptionResult(String steamAppId, Status status, Long gameId, String reason) {
    }

    record ExemptGame(long gameId, String title, String steamAppId) {
    }
}
