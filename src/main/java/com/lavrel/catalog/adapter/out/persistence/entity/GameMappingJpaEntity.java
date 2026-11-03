package com.lavrel.catalog.adapter.out.persistence.entity;

import jakarta.persistence.*;
import com.lavrel.shared.domain.Platform;

@Entity
@Table(name = "game_mapping", uniqueConstraints = {@UniqueConstraint(columnNames = {"platform", "platform_game_id"})})
public class GameMappingJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private long gameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false)
    private Platform platform;

    @Column(name = "platform_game_id", nullable = false)
    private String platformGameId;

    @Column(name = "platform_title")
    private String platformTitle;

    protected GameMappingJpaEntity() {
    }

    public GameMappingJpaEntity(Long id, long gameId, Platform platform, String platformGameId, String platformTitle) {
        this.id = id;
        this.gameId = gameId;
        this.platform = platform;
        this.platformGameId = platformGameId;
        this.platformTitle = platformTitle;
    }

    public Long getId() {
        return id;
    }

    public long getGameId() {
        return gameId;
    }

    public Platform getPlatform() {
        return platform;
    }

    public String getPlatformGameId() {
        return platformGameId;
    }

    public String getPlatformTitle() {
        return platformTitle;
    }
}
