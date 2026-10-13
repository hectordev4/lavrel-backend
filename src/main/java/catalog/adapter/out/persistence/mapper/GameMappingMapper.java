package catalog.adapter.out.persistence.mapper;

import catalog.adapter.out.persistence.entity.GameMappingJpaEntity;
import catalog.domain.GameMapping;

public final class GameMappingMapper {

    private GameMappingMapper() {
    }

    public static GameMapping toDomain(GameMappingJpaEntity e) {
        return new GameMapping(e.getId(), e.getGameId(), e.getPlatform(), e.getPlatformGameId(), e.getPlatformTitle());
    }

    public static GameMappingJpaEntity toEntity(GameMapping d) {
        return new GameMappingJpaEntity(d.id(), d.gameId(), d.platform(), d.platformGameId(), d.platformTitle());
    }
}
