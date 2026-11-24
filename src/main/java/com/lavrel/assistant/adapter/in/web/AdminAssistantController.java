package com.lavrel.assistant.adapter.in.web;

import com.lavrel.assistant.application.port.in.ManageWikiSourcesUseCase;
import com.lavrel.assistant.application.port.in.ManageWikiSourcesUseCase.GameWiki;
import com.lavrel.assistant.domain.ReferenceWiki;
import java.time.Instant;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin only. Reference wikis for the assistant (check the wiki's license allows reuse before adding it):
 *  - PUT    /api/admin/assistant/wikis/{gameId} {"apiUrl": "https://hades.fandom.com/api.php", "license": "CC BY-SA 3.0"}
 *  - GET    /api/admin/assistant/wikis
 *  - DELETE /api/admin/assistant/wikis/{gameId}
 *  - DELETE /api/admin/assistant/guides/games/{gameId}   forget the game's saved guides (they are written again)
 */
@RestController
public class AdminAssistantController {

    private final ManageWikiSourcesUseCase wikis;

    public AdminAssistantController(ManageWikiSourcesUseCase wikis) {
        this.wikis = wikis;
    }

    /**
     * PUT {"apiUrl": "https://darksouls.fandom.com/api.php"}. The backend asks the wiki for its name and license and
     * only accepts CC BY-SA. A "license" field is no longer needed (ignored if sent).
     */
    @PutMapping("/api/admin/assistant/wikis/{gameId}")
    public GameWikiResponse set(@PathVariable("gameId") @Positive long gameId,
                                @Valid @RequestBody WikiRequest request) {
        GameWiki result = wikis.approve(gameId, request.apiUrl());
        return new GameWikiResponse(result.gameId(), WikiResponse.from(result.wiki(), List.of(result.gameId())),
                result.guidesForgotten());
    }

    /** Every approved wiki once, with the games that use it. */
    @GetMapping("/api/admin/assistant/wikis")
    public WikisResponse list() {
        return new WikisResponse(wikis.list().stream()
                .map(w -> WikiResponse.from(w.wiki(), w.gameIds())).toList());
    }

    @DeleteMapping("/api/admin/assistant/wikis/{gameId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable("gameId") @Positive long gameId) {
        wikis.remove(gameId);
    }

    @DeleteMapping("/api/admin/assistant/guides/games/{gameId}")
    public ForgottenResponse forget(@PathVariable("gameId") @Positive long gameId) {
        return new ForgottenResponse(wikis.forgetGuides(gameId));
    }

    public record WikiRequest(@NotBlank @Size(max = 300) String apiUrl, @Size(max = 100) String license) {
    }

    public record WikiResponse(long id, String apiUrl, String siteName, String license, String licenseUrl,
                               Instant addedAt, List<Long> gameIds) {

        static WikiResponse from(ReferenceWiki w, List<Long> gameIds) {
            return new WikiResponse(w.id(), w.apiUrl(), w.siteName(), w.license(), w.licenseUrl(), w.addedAt(),
                    gameIds);
        }
    }

    public record GameWikiResponse(long gameId, WikiResponse wiki, int guidesForgotten) {
    }

    public record WikisResponse(List<WikiResponse> wikis) {
    }

    public record ForgottenResponse(int guidesForgotten) {
    }
}
