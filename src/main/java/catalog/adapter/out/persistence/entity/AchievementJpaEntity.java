package catalog.adapter.out.persistence.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "achievement", uniqueConstraints = {@UniqueConstraint(columnNames = {"game_id", "steam_achievement_id"})})
public class AchievementJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "game_id", nullable = false)
    private long gameId;

    @Column(name = "steam_achievement_id", nullable = false)
    private String steamAchievementId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "icon_url")
    private String iconUrl;

    @Column(name = "hidden", nullable = false)
    private boolean hidden;

    @Column(name = "updated_at")
    private Instant updatedAt;

    protected AchievementJpaEntity() {
    }

    public AchievementJpaEntity(Long id, long gameId, String steamAchievementId, String name, String description, String iconUrl, boolean hidden, Instant updatedAt) {
        this.id = id;
        this.gameId = gameId;
        this.steamAchievementId = steamAchievementId;
        this.name = name;
        this.description = description;
        this.iconUrl = iconUrl;
        this.hidden = hidden;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public long getGameId() {
        return gameId;
    }

    public String getSteamAchievementId() {
        return steamAchievementId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public boolean isHidden() {
        return hidden;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
