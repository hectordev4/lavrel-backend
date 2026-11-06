package com.lavrel.assistant.domain;

/** What the assistant knows about the achievement before looking anything up. Never anything about the player. */
public record AchievementContext(long gameId, String gameTitle, long achievementId, String name, String description,
                                 boolean hidden) {
}
