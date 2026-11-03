package com.lavrel.catalog.application.service;

import com.lavrel.catalog.application.port.in.GetGameUseCase;
import com.lavrel.catalog.application.port.out.GameMetadataRepositoryPort;
import com.lavrel.catalog.application.port.out.GameRepositoryPort;
import com.lavrel.catalog.domain.Game;
import com.lavrel.catalog.domain.GameFeature;
import com.lavrel.catalog.domain.GameMetadata;
import com.lavrel.catalog.domain.GameMetadata.Creator;
import com.lavrel.catalog.domain.GameMetadata.Feature;
import com.lavrel.catalog.domain.GameMetadata.FeatureGroup;
import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.catalog.domain.Slug;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GetGameService implements GetGameUseCase {

    private final GameRepositoryPort games;
    private final GameMetadataRepositoryPort metadata;

    public GetGameService(GameRepositoryPort games, GameMetadataRepositoryPort metadata) {
        this.games = games;
        this.metadata = metadata;
    }

    /** A game without details has not been checked against the content policy yet: it stays hidden. */
    @Override
    public GameDetails getGame(long gameId) {
        Game game = games.findById(gameId).orElseThrow(() -> new GameNotFoundException(gameId));
        GameMetadata details = metadata.find(gameId).orElseThrow(() -> new GameNotFoundException(gameId));

        Set<Integer> featureIds = details.features().stream().map(Feature::id).collect(Collectors.toSet());
        Map<String, Boolean> features = new LinkedHashMap<>();
        for (GameFeature feature : GameFeature.values()) {
            features.put(feature.apiKey(), featureIds.contains(feature.steamCategoryId()));
        }
        int accessibilityCount = (int) details.features().stream()
                .filter(f -> f.group() == FeatureGroup.ACCESSIBILITY)
                .count();

        String description = game.description() == null || game.description().isBlank() ? null : game.description();
        return new GameDetails(game.id(), game.title(), Slug.of(game.title()), game.coverImageUrl(), description,
                game.storeUrl(),
                details.releaseDate(),
                new GameDetails.Platforms(details.windows(), details.mac(), details.linux()),
                new GameDetails.Reviews(details.reviewCount(), details.reviewPercentPositive()),
                details.free(), details.requiredAge(),
                names(details.developers()), names(details.publishers()), names(details.franchises()),
                details.tags().stream()
                        .map(t -> new GameDetails.Tag(t.tagId(), t.name(), t.weight())).toList(),
                details.genres().stream().map(g -> new GameDetails.Genre(g.id(), g.name())).toList(),
                features, accessibilityCount,
                details.features().stream()
                        .map(f -> new GameDetails.FeatureInfo(f.id(), f.name(), f.group().name())).toList(),
                details.ageRatings().stream()
                        .map(r -> new GameDetails.AgeRatingInfo(r.board(), r.rating(), r.descriptors())).toList());
    }

    private static List<String> names(List<Creator> creators) {
        return creators.stream().map(Creator::name).toList();
    }
}
