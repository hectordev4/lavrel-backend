package com.lavrel.catalog.application.port.out;

import java.util.List;
import java.util.Optional;

/** The stored game-to-franchise links (game_creator rows with role FRANCHISE), for the admin clean-up. */
public interface FranchiseLinksPort {

    /** Every franchise link with the game's developers and publishers, to judge it. */
    List<FranchiseLink> findAll();

    /** Removes these links. @return how many */
    int unlink(List<FranchiseLink> links);

    /** Removes every franchise link of one creator. @return how many */
    int unlinkCreator(long creatorId);

    Optional<String> creatorName(long creatorId);

    record FranchiseLink(long gameId, long creatorId, String franchise, List<String> developers,
                         List<String> publishers) {
    }
}
