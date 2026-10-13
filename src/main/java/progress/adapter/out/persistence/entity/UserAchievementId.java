package progress.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public record UserAchievementId(
        @Column(name = "user_id", nullable = false) long userId,
        @Column(name = "achievement_id", nullable = false) long achievementId) implements Serializable {
}
