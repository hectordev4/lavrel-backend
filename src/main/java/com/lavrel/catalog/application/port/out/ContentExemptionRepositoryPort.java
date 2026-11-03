package com.lavrel.catalog.application.port.out;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Games an admin let into the catalog although the content policy would reject them (Marvel Rivals, for
 * example, whose user tags look adult). Stored as a flag on the game itself, so no extra table.
 */
public interface ContentExemptionRepositoryPort {

    void setExempt(long gameId, boolean exempt);

    /** The ids among gameIds that are exempt. */
    Set<Long> findExemptGameIds(Collection<Long> gameIds);

    List<ExemptGame> findAll();

    record ExemptGame(long gameId, String title, String steamAppId) {
    }
}
