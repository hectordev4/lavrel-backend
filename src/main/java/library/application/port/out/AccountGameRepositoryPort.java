package library.application.port.out;

import java.util.Collection;
import java.util.List;
import library.domain.AccountGame;

public interface AccountGameRepositoryPort {

    List<AccountGame> findByPlatformAccountId(long platformAccountId);

    /** Upserts by (platformAccountId, gameId). */
    void saveAll(Collection<AccountGame> accountGames);
}
