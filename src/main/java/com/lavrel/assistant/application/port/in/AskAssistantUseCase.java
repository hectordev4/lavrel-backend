package com.lavrel.assistant.application.port.in;

import com.lavrel.assistant.domain.AchievementNotFoundException;
import com.lavrel.assistant.domain.AssistantQuotaExceededException;
import com.lavrel.assistant.domain.AssistantUnavailableException;
import com.lavrel.assistant.domain.Guide.Source;
import com.lavrel.assistant.domain.InvalidQuestionException;
import java.time.Instant;
import java.util.List;

/**
 * The achievement guide assistant. Answers never contain anything about the player, only the game and the
 * achievement. Hidden achievements need the player's consent to spoilers first.
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
    Answer guide(long userId, long gameId, long achievementId, boolean spoilersAllowed, String language);

    /**
     * A follow-up question about the same achievement. Always a new answer (counted), never saved.
     *
     * @throws InvalidQuestionException if the question is empty or too long
     */
    Answer ask(long userId, long gameId, long achievementId, String question, boolean spoilersAllowed,
               String language);

    Usage usage(long userId);

    /** SPOILER_CONSENT_REQUIRED: hidden achievement, nothing was generated; ask again with spoilers allowed. */
    enum Status {
        READY, SPOILER_CONSENT_REQUIRED
    }

    /** saved: served from the saved guide (did not count). grounded: written from licensed reference text. */
    record Answer(Status status, String text, boolean grounded, List<Source> sources, boolean saved,
                  Instant createdAt) {

        public static Answer spoilerConsentRequired() {
            return new Answer(Status.SPOILER_CONSENT_REQUIRED, null, false, List.of(), false, null);
        }
    }

    record Usage(int usedToday, int dailyLimit) {
    }
}
