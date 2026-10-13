package progress.adapter.out.persistence.repository;

import progress.adapter.out.persistence.entity.UserAchievementJpaEntity;
import java.util.List;
import progress.adapter.out.persistence.entity.UserAchievementId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAchievementJpaRepository extends JpaRepository<UserAchievementJpaEntity, UserAchievementId> {

    List<UserAchievementJpaEntity> findByIdUserId(long userId);
}
