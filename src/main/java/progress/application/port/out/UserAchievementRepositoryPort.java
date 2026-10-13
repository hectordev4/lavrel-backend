package progress.application.port.out;

import java.util.Collection;
import java.util.List;
import progress.domain.UserAchievement;

public interface UserAchievementRepositoryPort {

    List<UserAchievement> findByUserId(long userId);

    /** Inserts the ones that do not exist yet. @return number actually inserted */
    int insertMissing(Collection<UserAchievement> unlocks);
}
