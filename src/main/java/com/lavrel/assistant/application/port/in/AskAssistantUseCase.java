package com.lavrel.assistant.application.port.in;

import com.lavrel.assistant.domain.AchievementNotFoundException;
import com.lavrel.assistant.domain.AssistantQuotaExceededException;
import com.lavrel.assistant.domain.AssistantUnavailableException;
import com.lavrel.assistant.domain.ChatMessage;
import com.lavrel.assistant.domain.Guide.Source;
import com.lavrel.assistant.domain.InvalidQuestionException;
import com.lavrel.catalog.domain.GameNotFoundException;
import java.time.Instant;
import java.util.List;

/**
 * The AI guide. Answers never contain anything about the player, only the game and the achievement. Hidden
 * achievements need the player's consent to spoilers first. Admins are not limited (unlimited requesters), so they can
 * refine guides; everyone else has a daily number of new answers.
 */
public interface AskAssistantUseCase {

    /**
     * The guide for an achievement: the saved one if it is still fresh (free), otherwise a new one (counts towards the
     * daily limit) that is saved for everyone.
     *
     * @throws AchievementNotFoundException     if the achievement is not in that published game
     * @throws AssistantQuotaExceededException  if a new answer is needed and the player's daily limit is used up
     * @throws AssistantUnavailableException    if the model cannot answer now
     */
    Answer guide(Requester requester, long gameId, long achievementId, boolean spoilersAllowed, String language);

    /**
     * A follow-up question about the same achievement. Always a new answer (counted), never saved.
     *
     * @throws InvalidQuestionException if the question is empty or too long
     */
    Answer ask(Requester requester, long gameId, long achievementId, String question, boolean spoilersAllowed,
               String language);

    /**
     * The open chat about a game. The conversation is kept by the frontend and sent each time (the last
     * {@value ChatMessage#MAX_MESSAGES} messages are used); nothing is stored. Counts like a follow-up.
     *
     * @throws GameNotFoundException    if the game is not published
     * @throws InvalidQuestionException if the conversation does not end with a question, or a message is too long
     */
    Answer chat(Requester requester, long gameId, List<ChatMessage> conversation, String language);

    Usage usage(Requester requester);

    /** Who asks: unlimited for admins (their answers are not counted). */
    record Requester(long userId, boolean unlimited) {
    }

    /** SPOILER_CONSENT_REQUIRED: hidden achievement, nothing was generated; ask again with spoilers allowed. */
    enum Status {
        READY, SPOILER_CONSENT_REQUIRED
    }

    /**
     * saved: served from the saved guide (did not count). grounded: written from licensed reference text, listed in
     * sources (title, url, license). curated: a guide an admin wrote or had rewritten; it never expires.
     */
    record Answer(Status status, String text, boolean grounded, List<Source> sources, boolean saved, boolean curated,
                  Instant createdAt) {

        public static Answer spoilerConsentRequired() {
            return new Answer(Status.SPOILER_CONSENT_REQUIRED, null, false, List.of(), false, false, null);
        }
    }

    /** dailyLimit is null for unlimited requesters. */
    record Usage(int usedToday, Integer dailyLimit, boolean unlimited) {
    }
}
