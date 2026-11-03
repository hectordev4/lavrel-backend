package com.lavrel.library.application.port.out;

import java.util.Collection;
import java.util.List;
import com.lavrel.library.domain.AccountGame;

public interface AccountGameRepositoryPort {

    List<AccountGame> findByPlatformAccountId(long platformAccountId);

    /** Upserts by (platformAccountId, gameId). */
    void saveAll(Collection<AccountGame> accountGames);
}
