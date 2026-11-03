package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.adapter.in.web.GameSearchController.GameCardResponse;
import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase;
import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.CreatorOption;
import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.CreatorRole;
import com.lavrel.catalog.application.port.in.BrowseFiltersUseCase.TagOption;
import com.lavrel.catalog.application.port.in.QuickSearchUseCase;
import com.lavrel.catalog.application.port.in.QuickSearchUseCase.QuickResults;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, no login needed:
 *  - GET /api/search?q=witch                       type-ahead: 5 games, 5 tags, 3 franchises
 *  - GET /api/tags?q=rogue                         tag options for the filters (q optional)
 *  - GET /api/creators?role=franchise&q=monster    franchise, developer or publisher options (q optional)
 */
@RestController
public class QuickSearchController {

    private final QuickSearchUseCase quickSearch;
    private final BrowseFiltersUseCase filters;

    public QuickSearchController(QuickSearchUseCase quickSearch, BrowseFiltersUseCase filters) {
        this.quickSearch = quickSearch;
        this.filters = filters;
    }

    @GetMapping("/api/search")
    public QuickSearchResponse search(@RequestParam("q") @NotNull @Size(min = 2, max = 100) String q) {
        QuickResults r = quickSearch.search(q);
        return new QuickSearchResponse(r.games().stream().map(GameCardResponse::from).toList(),
                r.tags().stream().map(TagResponse::from).toList(),
                r.franchises().stream().map(CreatorResponse::from).toList());
    }

    @GetMapping("/api/tags")
    public TagsResponse tags(@RequestParam(name = "q", required = false) @Size(max = 100) String q) {
        return new TagsResponse(filters.tags(q).stream().map(TagResponse::from).toList());
    }

    @GetMapping("/api/creators")
    public CreatorsResponse creators(
            @RequestParam("role") @Pattern(regexp = "(?i)franchise|developer|publisher") String role,
            @RequestParam(name = "q", required = false) @Size(max = 100) String q) {
        return new CreatorsResponse(filters.creators(CreatorRole.valueOf(role.toUpperCase(Locale.ROOT)), q).stream()
                .map(CreatorResponse::from).toList());
    }

    public record QuickSearchResponse(List<GameCardResponse> games, List<TagResponse> tags,
                                      List<CreatorResponse> franchises) {
    }

    public record TagsResponse(List<TagResponse> tags) {
    }

    public record CreatorsResponse(List<CreatorResponse> creators) {
    }

    /** Use id as the tag filter: GET /api/games?tag={id}. */
    public record TagResponse(long id, String name, int gameCount) {

        static TagResponse from(TagOption t) {
            return new TagResponse(t.id(), t.name(), t.gameCount());
        }
    }

    /** Use id with the matching filter: GET /api/games?franchise={id} (or developer=, publisher=). */
    public record CreatorResponse(long id, String name, CreatorRole role, int gameCount) {

        static CreatorResponse from(CreatorOption c) {
            return new CreatorResponse(c.id(), c.name(), c.role(), c.gameCount());
        }
    }
}
