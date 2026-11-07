package com.lavrel.library.application.port.in;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase.PlayerMode;
import com.lavrel.shared.domain.Platform;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * "My library": the games the user owns in any linked account (and that are published in the catalog), with
 * progress. Filters work like the catalog search; collectionId narrows it to one of the user's collections.
 */
public interface GetLibraryUseCase {

    /** @throws com.lavrel.library.domain.CollectionNotFoundException if collectionId is not one of the user's */
    LibraryPage library(long userId, LibraryCriteria criteria);

    /** The compact constructor applies the defaults and limits. */
    record LibraryCriteria(String query, List<Long> tagIds, Set<PlayerMode> modes, UUID collectionId, Sort sort,
                           int page, int size) {

        public static final int DEFAULT_SIZE = 48;
        public static final int MAX_SIZE = 96;
        public static final int MAX_TAGS = 5;

        public LibraryCriteria {
            query = query == null || query.isBlank() ? null : query.trim();
            tagIds = tagIds == null ? List.of() : tagIds.stream().distinct().limit(MAX_TAGS).toList();
            modes = modes == null || modes.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(modes));
            sort = sort == null ? Sort.RECENT : sort;
            page = Math.max(0, page);
            size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        }
    }

    /**
     * RECENT (default): last played first, never played last. NAME: alphabetical. PLAYTIME: most played first.
     * PROGRESS: highest share of achievements unlocked first. NEWEST: latest store release first.
     */
    enum Sort {
        RECENT, NAME, PLAYTIME, PROGRESS, NEWEST
    }

    record LibraryPage(List<LibraryCard> games, int page, int size, boolean hasMore) {
    }

    /**
     * One owned game. playtimeMinutes and lastPlayedAt add up all the user's accounts that own it; ownedOn lists
     * those platforms. trackingStatus is null when the game is not in the tracking list.
     */
    record LibraryCard(long gameId, String title, String slug, String coverImageUrl, LocalDate releaseDate, int playtimeMinutes,
                       Instant lastPlayedAt, List<Platform> ownedOn, int unlocked, int total, String trackingStatus,
                       boolean pinned) {
    }
}
