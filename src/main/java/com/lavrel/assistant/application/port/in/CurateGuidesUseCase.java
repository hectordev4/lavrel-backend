package com.lavrel.assistant.application.port.in;

import com.lavrel.assistant.application.port.in.AskAssistantUseCase.Answer;
import com.lavrel.assistant.domain.AchievementNotFoundException;
import com.lavrel.assistant.domain.InvalidQuestionException;

/**
 * Admin: refines the saved guide of an achievement. A curated guide is served to everyone, never expires and is never
 * replaced automatically; forgetting it lets the assistant write a new one.
 */
public interface CurateGuidesUseCase {

    /**
     * Writes the guide again with the admin's instructions ("more detail on the second phase", "mention the
     * difficulty requirement") and saves it as curated. Not counted.
     * @throws AchievementNotFoundException if the achievement is not in a published game
     * @throws InvalidQuestionException     if the instructions are empty or longer than 1000 characters
     */
    Answer rewrite(long achievementId, String language, String instructions);

    /** Saves the admin's own text as the curated guide (Markdown, up to 8000 characters). Sources are kept. */
    Answer edit(long achievementId, String language, String text);

    /** Forgets the saved guide, curated or not. @return false if there was none */
    boolean forget(long achievementId, String language);
}
