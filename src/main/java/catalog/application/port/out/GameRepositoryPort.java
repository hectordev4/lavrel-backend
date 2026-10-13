package catalog.application.port.out;

import catalog.domain.Game;
import java.util.Optional;

public interface GameRepositoryPort {

    Optional<Game> findById(long id);

    Game save(Game game);
}
