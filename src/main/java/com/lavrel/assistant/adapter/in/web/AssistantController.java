package com.lavrel.assistant.adapter.in.web;

import com.lavrel.assistant.application.port.in.AskAssistantUseCase;
import com.lavrel.assistant.application.port.in.AskAssistantUseCase.Answer;
import com.lavrel.assistant.application.port.in.AskAssistantUseCase.Usage;
import com.lavrel.assistant.domain.Questions;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The achievement guide assistant (signed in, e-mail verified):
 *  - GET  /api/games/{gameId}/achievements/{achievementId}/guide?spoilers=true&lang=es
 *         the saved guide (free) or a new one (counts towards the daily limit)
 *  - POST /api/games/{gameId}/achievements/{achievementId}/guide/questions
 *         {"question": "...", "spoilers": true, "lang": "es"}   a follow-up (counts, not saved)
 *  - GET  /api/currentuser/assistant/usage                      new answers used today and the daily limit
 * Hidden achievements answer status SPOILER_CONSENT_REQUIRED until spoilers=true. The text is Markdown: render it
 * as text, never as raw HTML.
 */
@RestController
public class AssistantController {

    private final AskAssistantUseCase assistant;

    public AssistantController(AskAssistantUseCase assistant) {
        this.assistant = assistant;
    }

    @GetMapping("/api/games/{gameId}/achievements/{achievementId}/guide")
    public Answer guide(@AuthenticationPrincipal AuthenticatedUser user,
                        @PathVariable("gameId") @Positive long gameId,
                        @PathVariable("achievementId") @Positive long achievementId,
                        @RequestParam(name = "spoilers", defaultValue = "false") boolean spoilers,
                        @RequestParam(name = "lang", required = false) @Size(max = 10) String lang) {
        return assistant.guide(user.userId(), gameId, achievementId, spoilers, lang);
    }

    @PostMapping("/api/games/{gameId}/achievements/{achievementId}/guide/questions")
    public Answer ask(@AuthenticationPrincipal AuthenticatedUser user,
                      @PathVariable("gameId") @Positive long gameId,
                      @PathVariable("achievementId") @Positive long achievementId,
                      @Valid @RequestBody QuestionRequest request) {
        return assistant.ask(user.userId(), gameId, achievementId, request.question(),
                Boolean.TRUE.equals(request.spoilers()), request.lang());
    }

    @GetMapping("/api/currentuser/assistant/usage")
    public Usage usage(@AuthenticationPrincipal AuthenticatedUser user) {
        return assistant.usage(user.userId());
    }

    public record QuestionRequest(@NotBlank @Size(max = Questions.MAX_LENGTH * 2) String question, Boolean spoilers,
                                  @Size(max = 10) String lang) {
    }
}
