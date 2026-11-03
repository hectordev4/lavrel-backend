package com.lavrel.progress.adapter.out.persistence;

import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.progress.adapter.out.persistence.mapper.UserAchievementMapper;
import com.lavrel.progress.adapter.out.persistence.repository.UserAchievementJpaRepository;
import com.lavrel.progress.application.port.out.UserAchievementRepositoryPort;
import com.lavrel.progress.domain.UserAchievement;

@Component
public class UserAchievementPersistenceAdapter implements UserAchievementRepositoryPort {

    private final UserAchievementJpaRepository repository;

    public UserAchievementPersistenceAdapter(UserAchievementJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserAchievement> findByUserId(long userId) {
        return repository.findByIdUserId(userId).stream().map(UserAchievementMapper::toDomain).toList();
    }

    /**
     * INSERT ... ON CONFLICT DO NOTHING per row: an existing (user, achievement) is never
     * overwritten or duplicated, and the affected-row count tells how many were really new.
     */
    @Override
    @Transactional
    public int insertMissing(Collection<UserAchievement> unlocks) {
        int inserted = 0;
        for (UserAchievement u : unlocks) {
            inserted += repository.insertIfAbsent(u.userId(), u.achievementId(), u.sourceAccountId(),
                    u.unlockedAt(), u.recordedAt());
        }
        return inserted;
    }
}
