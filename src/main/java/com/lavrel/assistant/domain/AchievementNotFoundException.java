package com.lavrel.assistant.domain;

/** Unknown achievement, achievement of another game, or game not published. */
public class AchievementNotFoundException extends RuntimeException {

    public AchievementNotFoundException() {
        super("Achievement not found");
    }
}
