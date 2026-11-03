package com.lavrel.catalog.application.port.out;

import com.lavrel.catalog.domain.GameMapping;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import com.lavrel.shared.domain.Platform;

public interface GameMappingRepositoryPort {

    List<GameMapping> findByPlatformAndPlatformGameIds(Platform platform, Collection<String> platformGameIds);

    Optional<GameMapping> findByGameIdAndPlatform(long gameId, Platform platform);

    GameMapping save(GameMapping mapping);
}
