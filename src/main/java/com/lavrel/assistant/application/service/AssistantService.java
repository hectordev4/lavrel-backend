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
import com.lavrel.assistant.domain.ChatMessage;
import com.lavrel.assistant.domain.Excerpt;
import com.lavrel.assistant.domain.Guide;
import com.lavrel.assistant.domain.GuidePrompts;
import com.lavrel.assistant.domain.Questions;
import com.lavrel.catalog.domain.GameNotFoundException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Order of every new answer: find the achievement or game, check spoilers, check the daily limits (skipped for
 * unlimited requesters), look up reference text, then ask the model. Saved guides skip the limits entirely. No
 * transaction around the model call (a network call).
 *
 * Counting happens before the model call, so a player over the limit never costs anything. If no answer comes back
 * (AssistantUnavailableException), the counts are given back: only answers the player actually receives are counted.
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
                            @Value("${app.assistant.global-daily-limit:200}") int globalDailyLimit,
                            @Value("${app.assistant.max-tokens:1400}") int maxTokens) {
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
    public Answer guide(Requester requester, long gameId, long achievementId, boolean spoilersAllowed,
                        String rawLanguage) {
        AchievementContext a = achievement(gameId, achievementId);
        if (a.hidden() && !spoilersAllowed) {
            return Answer.spoilerConsentRequired();
        }
        String language = Questions.language(rawLanguage);
        Instant now = clock.instant();
        Optional<Guide> saved = guides.find(achievementId, language).filter(g -> g.freshAt(now));
        if (saved.isPresent()) {
            Guide g = saved.get();
            return new Answer(Status.READY, g.text(), g.grounded(), g.sources(), true, g.curated(), g.createdAt());
        }
        LocalDate day = consume(requester);
        List<Excerpt> excerpts;
        String text;
        try {
            excerpts = references.find(a);
            text = model.complete(GuidePrompts.system(language), GuidePrompts.guide(a, excerpts), maxTokens);
        } catch (AssistantUnavailableException e) {
            refund(requester, day);
            throw e;
        }
        Guide guide = new Guide(achievementId, language, text, !excerpts.isEmpty(), sources(excerpts),
                model.modelName(), now, false);
        guides.save(guide);
        return new Answer(Status.READY, text, guide.grounded(), guide.sources(), false, false, now);
    }

    @Override
    public Answer ask(Requester requester, long gameId, long achievementId, String rawQuestion,
                      boolean spoilersAllowed, String rawLanguage) {
        String question = Questions.clean(rawQuestion);
        AchievementContext a = achievement(gameId, achievementId);
        if (a.hidden() && !spoilersAllowed) {
            return Answer.spoilerConsentRequired();
        }
        String language = Questions.language(rawLanguage);
        LocalDate day = consume(requester);
        List<Excerpt> excerpts;
        String text;
        try {
            excerpts = references.find(a);
            String savedGuide = guides.find(achievementId, language).map(Guide::text).orElse(null);
            text = model.complete(GuidePrompts.system(language),
                    GuidePrompts.followUp(a, excerpts, savedGuide, question), maxTokens);
        } catch (AssistantUnavailableException e) {
            refund(requester, day);
            throw e;
        }
        return new Answer(Status.READY, text, !excerpts.isEmpty(), sources(excerpts), false, false, clock.instant());
    }

    @Override
    public Answer chat(Requester requester, long gameId, List<ChatMessage> rawConversation, String rawLanguage) {
        List<ChatMessage> conversation = ChatMessage.validConversation(rawConversation);
        String gameTitle = achievements.publishedGameTitle(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
        String language = Questions.language(rawLanguage);
        LocalDate day = consume(requester);
        String question = conversation.get(conversation.size() - 1).content();
        List<Excerpt> excerpts;
        String text;
        try {
            excerpts = references.findForGame(gameId, question);
            List<ChatMessage> messages = new ArrayList<>();
            messages.add(new ChatMessage(ChatMessage.Role.USER, GuidePrompts.chatReferences(gameTitle, excerpts)));
            messages.add(new ChatMessage(ChatMessage.Role.ASSISTANT,
                    "Got it! Ask me anything about " + gameTitle + "."));
            messages.addAll(conversation);
            text = model.chat(GuidePrompts.chatSystem(language, gameTitle), messages, maxTokens);
        } catch (AssistantUnavailableException e) {
            refund(requester, day);
            throw e;
        }
        return new Answer(Status.READY, text, !excerpts.isEmpty(), sources(excerpts), false, false, clock.instant());
    }

    @Override
    public Usage usage(Requester requester) {
        int used = usage.used(userScope(requester.userId()), today());
        return requester.unlimited() ? new Usage(used, null, true) : new Usage(used, userDailyLimit, false);
    }

    private AchievementContext achievement(long gameId, long achievementId) {
        return achievements.find(gameId, achievementId).orElseThrow(AchievementNotFoundException::new);
    }

    /**
     * The player's limit first, so one heavy user cannot use up everybody's allowance. Unlimited requesters (admins)
     * are only counted, never stopped; Cloudflare's own free allowance still applies to them. If the global limit is
     * reached, the player's count is given back before refusing.
     *
     * @return the UTC day that was counted, so a refund gives back the same day even if the request crossed midnight
     */
    private LocalDate consume(Requester requester) {
        LocalDate day = today();
        String player = userScope(requester.userId());
        if (requester.unlimited()) {
            usage.tryConsume(player, day, Integer.MAX_VALUE);
            return day;
        }
        if (!usage.tryConsume(player, day, userDailyLimit)) {
            throw new AssistantQuotaExceededException(userDailyLimit);
        }
        if (!usage.tryConsume(EVERYONE, day, globalDailyLimit)) {
            usage.release(player, day);
            throw new AssistantUnavailableException("The assistant reached today's limit for everyone", null);
        }
        return day;
    }

    /** No answer reached the player: give back what consume() counted on that day. Admins only counted their own. */
    private void refund(Requester requester, LocalDate day) {
        usage.release(userScope(requester.userId()), day);
        if (!requester.unlimited()) {
            usage.release(EVERYONE, day);
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
