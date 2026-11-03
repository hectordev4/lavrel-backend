package com.lavrel.library.adapter.out.persistence.mapper;

import com.lavrel.library.adapter.out.persistence.entity.AccountGameJpaEntity;
import com.lavrel.library.domain.AccountGame;
import com.lavrel.library.adapter.out.persistence.entity.AccountGameId;

public final class AccountGameMapper {

    private AccountGameMapper() {
    }

    public static AccountGame toDomain(AccountGameJpaEntity e) {
        return new AccountGame(e.getId().platformAccountId(), e.getId().gameId(), e.getPlaytimeMinutes(),
                e.getLastPlayedAt(), e.getLastSyncedAt());
    }

    public static AccountGameJpaEntity toEntity(AccountGame d) {
        return new AccountGameJpaEntity(new AccountGameId(d.platformAccountId(), d.gameId()), d.playtimeMinutes(),
                d.lastPlayedAt(), d.lastSyncedAt());
    }
}
