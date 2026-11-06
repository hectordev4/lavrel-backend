package com.lavrel.assistant.adapter.in.web;

import com.lavrel.assistant.application.port.in.ManageWikiSourcesUseCase;
import com.lavrel.assistant.domain.WikiSource;
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

    @PutMapping("/api/admin/assistant/wikis/{gameId}")
    public WikiSource set(@PathVariable("gameId") @Positive long gameId, @Valid @RequestBody WikiRequest request) {
        return wikis.set(gameId, request.apiUrl(), request.license());
    }

    @GetMapping("/api/admin/assistant/wikis")
    public WikisResponse list() {
        return new WikisResponse(wikis.list());
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

    public record WikiRequest(@NotBlank @Size(max = 300) String apiUrl, @NotBlank @Size(max = 100) String license) {
    }

    public record WikisResponse(List<WikiSource> wikis) {
    }

    public record ForgottenResponse(int guidesForgotten) {
    }
}
