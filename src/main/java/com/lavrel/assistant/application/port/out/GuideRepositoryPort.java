package com.lavrel.assistant.application.port.out;

import com.lavrel.assistant.domain.Guide;
import java.util.Optional;

/** assistant_guide (migration V25). One guide per achievement and language. */
public interface GuideRepositoryPort {

    Optional<Guide> find(long achievementId, String language);

    /** Inserts or replaces. */
    void save(Guide guide);

    /** Forgets one saved guide. @return false if there was none */
    boolean delete(long achievementId, String language);

    /** Forgets every saved guide of a game (after its reference changed). @return how many */
    int deleteByGame(long gameId);
}
