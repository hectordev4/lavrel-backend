package catalog.adapter.out.persistence.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "achievement_mapping")
public class AchievementMappingJpaEntity {

    @EmbeddedId
    private AchievementMappingId id;

    @Column(name = "platform_achievement_id")
    private String platformAchievementId;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    protected AchievementMappingJpaEntity() {
    }

    public AchievementMappingJpaEntity(AchievementMappingId id, String platformAchievementId, boolean available) {
        this.id = id;
        this.platformAchievementId = platformAchievementId;
        this.available = available;
    }

    public AchievementMappingId getId() {
        return id;
    }

    public String getPlatformAchievementId() {
        return platformAchievementId;
    }

    public boolean isAvailable() {
        return available;
    }
}
