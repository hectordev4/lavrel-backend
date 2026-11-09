package com.lavrel.assistant.application.service;

import com.lavrel.assistant.application.port.in.AskAssistantUseCase.Answer;
import com.lavrel.assistant.application.port.in.AskAssistantUseCase.Status;
import com.lavrel.assistant.application.port.in.CurateGuidesUseCase;
import com.lavrel.assistant.application.port.out.AchievementContextPort;
import com.lavrel.assistant.application.port.out.GuideRepositoryPort;
import com.lavrel.assistant.application.port.out.LanguageModelPort;
import com.lavrel.assistant.application.port.out.ReferencePort;
import com.lavrel.assistant.domain.AchievementContext;
import com.lavrel.assistant.domain.AchievementNotFoundException;
import com.lavrel.assistant.domain.Excerpt;
import com.lavrel.assistant.domain.Guide;
import com.lavrel.assistant.domain.GuidePrompts;
import com.lavrel.assistant.domain.InvalidQuestionException;
import com.lavrel.assistant.domain.Questions;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CurateGuidesService implements CurateGuidesUseCase {

    private static final Logger log = LoggerFactory.getLogger(CurateGuidesService.class);
    static final int MAX_INSTRUCTIONS = 1000;
    static final int MAX_TEXT = 8000;
    static final String EDITED_BY_STAFF = "edited by staff";

    private final AchievementContextPort achievements;
    private final GuideRepositoryPort guides;
    private final ReferencePort references;
    private final LanguageModelPort model;
    private final Clock clock;
    private final int maxTokens;

    public CurateGuidesService(AchievementContextPort achievements, GuideRepositoryPort guides,
                               ReferencePort references, LanguageModelPort model, Clock clock,
                               @Value("${app.assistant.curated-max-tokens:1500}") int maxTokens) {
        this.achievements = achievements;
        this.guides = guides;
        this.references = references;
        this.model = model;
        this.clock = clock;
        this.maxTokens = maxTokens;
    }

    @Override
    public Answer rewrite(long achievementId, String rawLanguage, String instructions) {
        if (instructions == null || instructions.isBlank() || instructions.length() > MAX_INSTRUCTIONS) {
            throw new InvalidQuestionException("Instructions are required, at most " + MAX_INSTRUCTIONS + " characters");
        }
        AchievementContext a = achievement(achievementId);
        String language = Questions.language(rawLanguage);
        List<Excerpt> excerpts = references.find(a);
        String text = model.complete(GuidePrompts.system(language),
                GuidePrompts.curatedGuide(a, excerpts, instructions.trim()), maxTokens);
        Instant now = clock.instant();
        Guide guide = new Guide(achievementId, language, text, !excerpts.isEmpty(),
                excerpts.stream().map(e -> new Guide.Source(e.title(), e.url(), e.license())).distinct().toList(),
                model.modelName(), now, true);
        guides.save(guide);
        log.info("Guide of achievement {} ({}) rewritten by an admin", achievementId, language);
        return answer(guide);
    }

    @Override
    public Answer edit(long achievementId, String rawLanguage, String text) {
        if (text == null || text.isBlank() || text.length() > MAX_TEXT) {
            throw new InvalidQuestionException("The guide text is required, at most " + MAX_TEXT + " characters");
        }
        achievement(achievementId);
        String language = Questions.language(rawLanguage);
        Guide previous = guides.find(achievementId, language).orElse(null);
        Guide guide = new Guide(achievementId, language, text.trim(), previous != null && previous.grounded(),
                previous == null ? List.of() : previous.sources(), EDITED_BY_STAFF, clock.instant(), true);
        guides.save(guide);
        log.info("Guide of achievement {} ({}) edited by an admin", achievementId, language);
        return answer(guide);
    }

    @Override
    public boolean forget(long achievementId, String rawLanguage) {
        return guides.delete(achievementId, Questions.language(rawLanguage));
    }

    private AchievementContext achievement(long achievementId) {
        return achievements.findById(achievementId).orElseThrow(AchievementNotFoundException::new);
    }

    private static Answer answer(Guide g) {
        return new Answer(Status.READY, g.text(), g.grounded(), g.sources(), true, true, g.createdAt());
    }
}
