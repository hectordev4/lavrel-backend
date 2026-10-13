package progress.application.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import progress.application.port.in.RecordUnlocksUseCase;
import progress.application.port.out.UserAchievementRepositoryPort;
import progress.domain.UserAchievement;

@Service
public class RecordUnlocksService implements RecordUnlocksUseCase {

    private final UserAchievementRepositoryPort repository;
    private final Clock clock;

    public RecordUnlocksService(UserAchievementRepositoryPort repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public int record(Command command) {
        if (command.unlocks().isEmpty()) {
            return 0;
        }
        Instant now = clock.instant();
        List<UserAchievement> rows = command.unlocks().stream()
                .map(u -> new UserAchievement(command.userId(), u.achievementId(),
                        command.sourceAccountId(), u.unlockedAt(), now))
                .toList();
        return repository.insertMissing(rows);
    }
}
