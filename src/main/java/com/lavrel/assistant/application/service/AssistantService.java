package com.lavrel.assistant.application.service;

import com.lavrel.assistant.application.port.in.AskAssistantUseCase;
import com.lavrel.assistant.application.port.out.AchievementContextPort;
import com.lavrel.assistant.application.port.out.AssistantUsagePort;
import com.lavrel.assistant.application.port.out.GuideRepositoryPort;
import com.lavrel.assistant.application.port.out.LanguageModelPort;
import com.lavrel.assistant.application.port.out.ReferencePort;
import com.lavrel.assistant.domain.AchievementContext;
import com.lavrel.assistant.domain.AchievementNotFoundException;
import com.lavrel.assistant.domain.AssistantQuotaExceededException;
import com.lavrel.assistant.domain.AssistantUnavailableException;
import com.lavrel.assistant.domain.Excerpt;
import com.lavrel.assistant.domain.Guide;
import com.lavrel.assistant.domain.GuidePrompts;
import com.lavrel.assistant.domain.Questions;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Order of every new answer: find the achievement, check spoilers, check the daily limits, look up reference text,
 * then ask the model. Saved guides skip the limits entirely. No transaction around the model call (a network call).
 */
@Service
public class AssistantService implements AskAssistantUseCase {

    static final String EVERYONE = "all";

    private final AchievementContextPort achievements;
    private final GuideRepositoryPort guides;
    private final ReferencePort references;
    private final LanguageModelPort model;
    private final AssistantUsagePort usage;
    private final Clock clock;
    private final int userDailyLimit;
    private final int globalDailyLimit;
    private final int maxTokens;

    public AssistantService(AchievementContextPort achievements, GuideRepositoryPort guides,
                            ReferencePort references, LanguageModelPort model, AssistantUsagePort usage, Clock clock,
                            @Value("${app.assistant.user-daily-limit:20}") int userDailyLimit,
                            @Value("${app.assistant.global-daily-limit:400}") int globalDailyLimit,
                            @Value("${app.assistant.max-tokens:600}") int maxTokens) {
        this.achievements = achievements;
        this.guides = guides;
        this.references = references;
        this.model = model;
        this.usage = usage;
        this.clock = clock;
        this.userDailyLimit = userDailyLimit;
        this.globalDailyLimit = globalDailyLimit;
        this.maxTokens = maxTokens;
    }

    @Override
    public Answer guide(long userId, long gameId, long achievementId, boolean spoilersAllowed, String rawLanguage) {
        AchievementContext a = achievement(gameId, achievementId);
        if (a.hidden() && !spoilersAllowed) {
            return Answer.spoilerConsentRequired();
        }
        String language = Questions.language(rawLanguage);
        Instant now = clock.instant();
        Optional<Guide> saved = guides.find(achievementId, language).filter(g -> g.freshAt(now));
        if (saved.isPresent()) {
            Guide g = saved.get();
            return new Answer(Status.READY, g.text(), g.grounded(), g.sources(), true, g.createdAt());
        }
        consume(userId);
        List<Excerpt> excerpts = references.find(a);
        String text = model.complete(GuidePrompts.system(language), GuidePrompts.guide(a, excerpts), maxTokens);
        Guide guide = new Guide(achievementId, language, text, !excerpts.isEmpty(), sources(excerpts),
                model.modelName(), now);
        guides.save(guide);
        return new Answer(Status.READY, text, guide.grounded(), guide.sources(), false, now);
    }

    @Override
    public Answer ask(long userId, long gameId, long achievementId, String rawQuestion, boolean spoilersAllowed,
                      String rawLanguage) {
        String question = Questions.clean(rawQuestion);
        AchievementContext a = achievement(gameId, achievementId);
        if (a.hidden() && !spoilersAllowed) {
            return Answer.spoilerConsentRequired();
        }
        String language = Questions.language(rawLanguage);
        consume(userId);
        List<Excerpt> excerpts = references.find(a);
        String savedGuide = guides.find(achievementId, language).map(Guide::text).orElse(null);
        String text = model.complete(GuidePrompts.system(language),
                GuidePrompts.followUp(a, excerpts, savedGuide, question), maxTokens);
        return new Answer(Status.READY, text, !excerpts.isEmpty(), sources(excerpts), false, clock.instant());
    }

    @Override
    public Usage usage(long userId) {
        return new Usage(usage.used(userScope(userId), today()), userDailyLimit);
    }

    private AchievementContext achievement(long gameId, long achievementId) {
        return achievements.find(gameId, achievementId).orElseThrow(AchievementNotFoundException::new);
    }

    /** The player's limit first, so one heavy user cannot use up everybody's allowance. */
    private void consume(long userId) {
        LocalDate day = today();
        if (!usage.tryConsume(userScope(userId), day, userDailyLimit)) {
            throw new AssistantQuotaExceededException(userDailyLimit);
        }
        if (!usage.tryConsume(EVERYONE, day, globalDailyLimit)) {
            throw new AssistantUnavailableException("The assistant reached today's limit for everyone", null);
        }
    }

    private LocalDate today() {
        return LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
    }

    private static String userScope(long userId) {
        return "user:" + userId;
    }

    private static List<Guide.Source> sources(List<Excerpt> excerpts) {
        return excerpts.stream().map(e -> new Guide.Source(e.title(), e.url(), e.license())).distinct().toList();
    }
}
