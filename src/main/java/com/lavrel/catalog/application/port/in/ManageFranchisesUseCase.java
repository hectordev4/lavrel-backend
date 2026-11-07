package com.lavrel.catalog.application.port.in;

import com.lavrel.catalog.application.port.out.FranchiseBlocklistPort.BlockedFranchise;
import java.util.List;

/**
 * Admin: keeps the franchise filter clean. Steam's franchise field often holds studio names, store brands or
 * placeholders; the import already skips them (FranchiseRules), and this cleans what was stored before.
 */
public interface ManageFranchisesUseCase {

    /** Lists (and with apply=true removes) the stored franchise links the rules reject. */
    CleanupReport cleanup(boolean apply);

    /**
     * Marks a creator as not a franchise: removes all its franchise links and skips it in future imports.
     * @throws com.lavrel.catalog.domain.CreatorNotFoundException if there is no such creator
     */
    BlockedFranchise block(long creatorId);

    /** Undoes block; its links come back when the games are refreshed. */
    void unblock(long creatorId);

    List<BlockedFranchise> blocked();

    record CleanupReport(boolean applied, int linksRemoved, List<Finding> findings) {
    }

    /** One rejected franchise: how many games carry it and why it is rejected (the first reason found). */
    record Finding(long creatorId, String name, int games, String reason) {
    }
}
