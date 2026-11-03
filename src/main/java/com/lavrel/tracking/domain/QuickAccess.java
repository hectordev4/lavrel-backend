package com.lavrel.tracking.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * The rules of a user's quick access: the pinned games of the tracking list, at most {@value #MAX_PINNED}, in an
 * order the user chooses, numbered 1, 2, 3... without gaps. Pure functions over the user's pinned entries.
 */
public final class QuickAccess {

    public static final int MAX_PINNED = 20;

    private QuickAccess() {
    }

    /**
     * Pins the entry at the end.
     * @param pinned the user's currently pinned entries, in order
     * @throws QuickAccessFullException if {@value #MAX_PINNED} games are pinned already
     */
    public static void pinAtEnd(TrackedGame entry, List<TrackedGame> pinned, Instant now) {
        if (entry.pinned()) {
            return;
        }
        if (pinned.size() >= MAX_PINNED) {
            throw new QuickAccessFullException(MAX_PINNED);
        }
        entry.pinAt(pinned.size() + 1, now);
    }

    /**
     * Renumbers the remaining pinned entries 1..n after one left quick access.
     * @return the entries whose position changed (to be saved)
     */
    public static List<TrackedGame> closeGaps(List<TrackedGame> remainingPinned, Instant now) {
        List<TrackedGame> changed = new ArrayList<>();
        int position = 1;
        for (TrackedGame entry : remainingPinned) {
            if (entry.pinPosition() == null || entry.pinPosition() != position) {
                entry.pinAt(position, now);
                changed.add(entry);
            }
            position++;
        }
        return changed;
    }

    /**
     * Puts the pinned entries in the given order.
     * @param gameIds every pinned game exactly once, in the new order
     * @return the entries whose position changed (to be saved)
     * @throws InvalidQuickAccessOrderException if the list is not exactly the pinned games
     */
    public static List<TrackedGame> reorder(List<TrackedGame> pinned, List<Long> gameIds, Instant now) {
        if (gameIds.size() != new HashSet<>(gameIds).size()) {
            throw new InvalidQuickAccessOrderException("A game appears more than once");
        }
        Map<Long, TrackedGame> byGame = pinned.stream()
                .collect(Collectors.toMap(TrackedGame::gameId, Function.identity()));
        if (!byGame.keySet().equals(new HashSet<>(gameIds))) {
            throw new InvalidQuickAccessOrderException("The order must list exactly the pinned games");
        }
        return closeGaps(gameIds.stream().map(byGame::get).toList(), now);
    }
}
