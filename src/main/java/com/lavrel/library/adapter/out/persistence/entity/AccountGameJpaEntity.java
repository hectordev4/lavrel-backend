package com.lavrel.library.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "account_game")
public class AccountGameJpaEntity {

    @EmbeddedId
    private AccountGameId id;

    @Column(name = "playtime_minutes", nullable = false)
    private int playtimeMinutes;

    @Column(name = "last_played_at")
    private Instant lastPlayedAt;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    protected AccountGameJpaEntity() {
    }

    public AccountGameJpaEntity(AccountGameId id, int playtimeMinutes, Instant lastPlayedAt, Instant lastSyncedAt) {
        this.id = id;
        this.playtimeMinutes = playtimeMinutes;
        this.lastPlayedAt = lastPlayedAt;
        this.lastSyncedAt = lastSyncedAt;
    }

    public AccountGameId getId() {
        return id;
    }

    public int getPlaytimeMinutes() {
        return playtimeMinutes;
    }

    public Instant getLastPlayedAt() {
        return lastPlayedAt;
    }

    public Instant getLastSyncedAt() {
        return lastSyncedAt;
    }
}
