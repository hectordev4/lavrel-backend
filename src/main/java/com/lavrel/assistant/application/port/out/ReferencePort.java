package com.lavrel.assistant.application.port.out;

import com.lavrel.assistant.domain.AchievementContext;
import com.lavrel.assistant.domain.Excerpt;
import java.util.List;

/**
 * Finds reference text about an achievement from sources whose license allows reuse. Empty when there is none, in
 * which case the model answers from its own knowledge and the answer is marked as not grounded.
 */
public interface ReferencePort {

    List<Excerpt> find(AchievementContext achievement);

    /** Reference text for a free question about a game (its approved wiki, searched with the question). */
    List<Excerpt> findForGame(long gameId, String question);
}
