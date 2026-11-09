package com.lavrel.assistant.adapter.in.web;

import com.lavrel.assistant.application.port.in.AskAssistantUseCase.Answer;
import com.lavrel.assistant.application.port.in.CurateGuidesUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin only. Refining the saved guide of one achievement (curated guides never expire):
 *  - POST   /api/admin/assistant/guides/achievements/{id}/rewrite {"lang": "en", "instructions": "More detail on..."}
 *  - PUT    /api/admin/assistant/guides/achievements/{id}         {"lang": "en", "text": "..."}   your own text
 *  - DELETE /api/admin/assistant/guides/achievements/{id}?lang=en                                 forget it
 */
@RestController
@RequestMapping("/api/admin/assistant/guides/achievements/{achievementId}")
public class AdminGuidesController {

    private final CurateGuidesUseCase curate;

    public AdminGuidesController(CurateGuidesUseCase curate) {
        this.curate = curate;
    }

    @PostMapping("/rewrite")
    public Answer rewrite(@PathVariable("achievementId") @Positive long achievementId,
                          @Valid @RequestBody RewriteRequest request) {
        return curate.rewrite(achievementId, request.lang(), request.instructions());
    }

    @PutMapping
    public Answer edit(@PathVariable("achievementId") @Positive long achievementId,
                       @Valid @RequestBody EditRequest request) {
        return curate.edit(achievementId, request.lang(), request.text());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forget(@PathVariable("achievementId") @Positive long achievementId,
                       @RequestParam(name = "lang", defaultValue = "en") @Size(max = 10) String lang) {
        curate.forget(achievementId, lang);
    }

    public record RewriteRequest(@Size(max = 10) String lang, @NotBlank @Size(max = 1000) String instructions) {
    }

    public record EditRequest(@Size(max = 10) String lang, @NotBlank @Size(max = 8000) String text) {
    }
}
