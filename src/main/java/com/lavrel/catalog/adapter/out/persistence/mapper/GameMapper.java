package com.lavrel.catalog.adapter.out.persistence.mapper;

import com.lavrel.catalog.adapter.out.persistence.entity.GameJpaEntity;
import com.lavrel.catalog.domain.Game;

public final class GameMapper {

    private GameMapper() {
    }

    public static Game toDomain(GameJpaEntity e) {
        return new Game(e.getId(), e.getTitle(), e.getCoverImageUrl(), e.getDescription(),
                e.getStoreUrl(), e.getCreatedAt());
    }

    public static GameJpaEntity toEntity(Game d) {
        return new GameJpaEntity(d.id(), d.title(), d.coverImageUrl(), d.description(),
                d.storeUrl(), d.createdAt());
    }
}
