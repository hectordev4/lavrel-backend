package com.lavrel.library.adapter.in.web;

import com.lavrel.catalog.application.port.in.SearchGamesUseCase.PlayerMode;
import com.lavrel.library.application.port.in.GetLibraryUseCase;
import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryCard;
import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryCriteria;
import com.lavrel.library.application.port.in.GetLibraryUseCase.LibraryPage;
import com.lavrel.library.application.port.in.GetLibraryUseCase.Sort;
import com.lavrel.shared.domain.Platform;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/currentuser/games: the games the user owns (all linked accounts), for the "My library" grid.
 *  - q, tag (up to 5, all must apply), mode: like GET /api/games; collection: one of the user's collections
 *  - sort: recent (default, last played first), name, playtime, progress, newest
 *  - page (from 0) and size (default 48, at most 96); hasMore tells whether a next page exists
 */
@RestController
public class LibraryController {

    private final GetLibraryUseCase library;

    public LibraryController(GetLibraryUseCase library) {
        this.library = library;
    }

    @GetMapping("/api/currentuser/games")
    public LibraryResponse library(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(name = "q", required = false) @Size(min = 2, max = 100) String q,
            @RequestParam(name = "tag", required = false)
            @Size(max = LibraryCriteria.MAX_TAGS) List<@NotNull @Positive Long> tagIds,
            @RequestParam(name = "mode", required = false)
            List<@Pattern(regexp = "(?i)single-player|co-op|multiplayer") String> modes,
            @RequestParam(name = "collection", required = false) UUID collectionId,
            @RequestParam(name = "sort", required = false)
            @Pattern(regexp = "(?i)recent|name|playtime|progress|newest") String sort,
            @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(1000) int page,
            @RequestParam(name = "size", defaultValue = "48") @Min(1) @Max(LibraryCriteria.MAX_SIZE) int size) {
        LibraryPage result = library.library(user.userId(), new LibraryCriteria(q, tagIds, parseModes(modes), collectionId,
                sort == null ? null : Sort.valueOf(sort.toUpperCase(Locale.ROOT)), page, size));
        return new LibraryResponse(result.games().stream().map(LibraryGameResponse::from).toList(), result.page(),
                result.size(), result.hasMore());
    }

    private static Set<PlayerMode> parseModes(List<String> modes) {
        if (modes == null) {
            return Set.of();
        }
        return modes.stream()
                .map(m -> PlayerMode.valueOf(m.toUpperCase(Locale.ROOT).replace('-', '_')))
                .collect(Collectors.toSet());
    }

    public record LibraryResponse(List<LibraryGameResponse> games, int page, int size, boolean hasMore) {
    }

    /** slug is the page address (/games/{slug}); trackingStatus is null when not tracked. */
    public record LibraryGameResponse(long gameId, String title, String slug, String coverImageUrl,
                                      LocalDate releaseDate, int playtimeMinutes, Instant lastPlayedAt,
                                      List<Platform> ownedOn, int unlocked, int total, String trackingStatus,
                                      boolean pinned) {

        static LibraryGameResponse from(LibraryCard c) {
            return new LibraryGameResponse(c.gameId(), c.title(), c.slug(), c.coverImageUrl(),
                    c.releaseDate(), c.playtimeMinutes(), c.lastPlayedAt(), c.ownedOn(), c.unlocked(), c.total(),
                    c.trackingStatus(), c.pinned());
        }
    }
}
