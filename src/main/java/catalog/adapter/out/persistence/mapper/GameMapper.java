package catalog.adapter.out.persistence.mapper;

import catalog.adapter.out.persistence.entity.GameJpaEntity;
import catalog.domain.Game;

public final class GameMapper {

    private GameMapper() {
    }

    public static Game toDomain(GameJpaEntity e) {
        return new Game(e.getId(), e.getTitle(), e.getCoverImageUrl(), e.getCreatedAt());
    }

    public static GameJpaEntity toEntity(Game d) {
        return new GameJpaEntity(d.id(), d.title(), d.coverImageUrl(), d.createdAt());
    }
}
