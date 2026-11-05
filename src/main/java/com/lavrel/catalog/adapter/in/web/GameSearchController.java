package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameCard;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GamePage;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.GameSearchCriteria;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.PlayerMode;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase.Sort;
import com.lavrel.catalog.application.port.in.SearchGamesUseCase;
import com.lavrel.catalog.domain.GameSystem;
import com.lavrel.catalog.domain.Slug;
import com.lavrel.shared.web.PublicCache;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public game search (no login needed): GET /api/games?q=witcher&tag=19,492&franchise=12&mode=co-op&sort=popular
 *  - q: 2-100 characters, typo tolerant; tag: up to 5 tag ids (all must apply); franchise / developer / publisher:
 *    creator ids; mode: single-player, co-op, multiplayer (all must be offered); system: pc, xbox,
 *    playstation (available on all of them)
 *  - sort: relevance (default with q), popular (default without q), rating, newest, name
 *  - page (from 0) and size (default 20, at most 50); hasMore tells whether a next page exists
 * Browse pages are this endpoint with a preset filter (e.g. ?tag=1716 for Roguelike).
 */
@RestController
@RequestMapping("/api/games")
public class GameSearchController {

    private final SearchGamesUseCase searchGames;

    public GameSearchController(SearchGamesUseCase searchGames) {
        this.searchGames = searchGames;
    }

    @GetMapping
    public ResponseEntity<SearchResponse> search(
            @RequestParam(name = "q", required = false) @Size(min = 2, max = 100) String q,
            @RequestParam(name = "tag", required = false)
            @Size(max = GameSearchCriteria.MAX_TAGS) List<@NotNull @Positive Long> tagIds,
            @RequestParam(name = "franchise", required = false) @Positive Long franchiseId,
            @RequestParam(name = "developer", required = false) @Positive Long developerId,
            @RequestParam(name = "publisher", required = false) @Positive Long publisherId,
            @RequestParam(name = "mode", required = false)
            List<@Pattern(regexp = "(?i)single-player|co-op|multiplayer") String> modes,
            @RequestParam(name = "system", required = false) Set<GameSystem> systems,
            @RequestParam(name = "sort", required = false)
            @Pattern(regexp = "(?i)relevance|popular|rating|newest|name") String sort,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(1000) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(GameSearchCriteria.MAX_SIZE) int size) {
        GamePage result = searchGames.search(new GameSearchCriteria(q, tagIds, franchiseId, developerId, publisherId,
                parseModes(modes), systems, sort == null ? null : Sort.valueOf(sort.toUpperCase(Locale.ROOT)), page, size));
        return ResponseEntity.ok().cacheControl(PublicCache.SEARCH).body(new SearchResponse(
                result.games().stream().map(GameCardResponse::from).toList(), result.page(), result.size(),
                result.hasMore()));
    }

    private static Set<PlayerMode> parseModes(List<String> modes) {
        if (modes == null) {
            return Set.of();
        }
        return modes.stream()
                .map(m -> PlayerMode.valueOf(m.toUpperCase(Locale.ROOT).replace('-', '_')))
                .collect(Collectors.toSet());
    }

    public record SearchResponse(List<GameCardResponse> games, int page, int size, boolean hasMore) {
    }

    /** slug is for the page address: /games/{id}-{slug}. */
    public record GameCardResponse(long id, String title, String slug, String coverImageUrl, LocalDate releaseDate,
                                   Platforms platforms, Integer reviewCount, Integer reviewPercentPositive,
                                   List<String> topTags, Modes modes, List<GameSystem> availableOn) {

        static GameCardResponse from(GameCard g) {
            return new GameCardResponse(g.id(), g.title(), Slug.of(g.title()), g.coverImageUrl(), g.releaseDate(),
                    new Platforms(g.windows(), g.mac(), g.linux()), g.reviewCount(), g.reviewPercentPositive(),
                    g.topTags(), new Modes(g.singlePlayer(), g.coOp(), g.multiplayer()), g.availableOn());
        }
    }

    public record Platforms(boolean windows, boolean mac, boolean linux) {
    }

    public record Modes(boolean singlePlayer, boolean coOp, boolean multiplayer) {
    }
}
