package com.lavrel.catalog.application.port.in;

import com.lavrel.catalog.domain.GameSystem;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * The full game search behind the results page and the browse pages ("all Roguelike games", "the Monster Hunter
 * series" are this search with a filter preset). Public: only published, safe-for-work games are ever returned.
 */
public interface SearchGamesUseCase {

    GamePage search(GameSearchCriteria criteria);

    /**
     * query: free text, matched on the game name ignoring case, accents and punctuation, tolerating typos.
     * tagIds: every tag must apply to the game (AND), counting only tags that really describe it (see the query).
     * franchiseId / developerId / publisherId: creator ids from GET /api/creators. modes: every mode must be offered.
     * systems: the game must be available on every one of them.
     * The compact constructor applies the defaults and limits, so the rest of the code never sees invalid values.
     */
    record GameSearchCriteria(String query, List<Long> tagIds, Long franchiseId, Long developerId, Long publisherId,
                              Set<PlayerMode> modes, Set<GameSystem> systems, Sort sort, int page, int size) {

        public static final int DEFAULT_SIZE = 20;
        public static final int MAX_SIZE = 50;
        public static final int MAX_TAGS = 5;

        public GameSearchCriteria {
            query = query == null || query.isBlank() ? null : query.trim();
            tagIds = tagIds == null ? List.of() : tagIds.stream().distinct().limit(MAX_TAGS).toList();
            modes = modes == null || modes.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(modes));
            systems = systems == null || systems.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(systems));
            if (sort == null || (sort == Sort.RELEVANCE && query == null)) {
                sort = query == null ? Sort.POPULAR : Sort.RELEVANCE;
            }
            page = Math.max(0, page);
            size = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        }
    }

    /** Steam store categories behind each mode. */
    enum PlayerMode {
        SINGLE_PLAYER(2), CO_OP(9, 38), MULTIPLAYER(1, 27);

        private final List<Integer> featureIds;

        PlayerMode(Integer... featureIds) {
            this.featureIds = List.of(featureIds);
        }

        public List<Integer> featureIds() {
            return featureIds;
        }
    }

    /**
     * RELEVANCE (default with a query): exact name, then names starting with it, containing it, then close matches.
     * POPULAR (default without a query): most reviews. RATING: best review score among games with enough reviews.
     * NEWEST: latest store release. NAME: alphabetical.
     */
    enum Sort {
        RELEVANCE, POPULAR, RATING, NEWEST, NAME
    }

    /** hasMore instead of a total: exact totals are slow on fuzzy searches. */
    record GamePage(List<GameCard> games, int page, int size, boolean hasMore) {
    }

    /**
     * Enough for a result card without opening the game page. topTags: the 3 most applied tags; availableOn: the
     * systems it is sold on.
     */
    record GameCard(long id, String title, String slug, String coverImageUrl, LocalDate releaseDate, boolean windows,
                    boolean mac, boolean linux, Integer reviewCount, Integer reviewPercentPositive,
                    List<String> topTags, boolean singlePlayer, boolean coOp, boolean multiplayer,
                    List<GameSystem> availableOn) {
    }
}
