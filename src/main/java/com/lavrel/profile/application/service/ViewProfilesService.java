package com.lavrel.profile.application.service;

import com.lavrel.profile.application.port.in.ViewProfilesUseCase;
import com.lavrel.profile.application.port.out.PublicProfileQueryPort;
import com.lavrel.profile.application.port.out.ShelfContentQueryPort;
import com.lavrel.profile.application.port.out.ShelfRepositoryPort;
import com.lavrel.profile.domain.ProfileNotFoundException;
import com.lavrel.profile.domain.Shelf;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViewProfilesService implements ViewProfilesUseCase {

    static final int PAGE_SIZE = 20;
    private static final Pattern PUBLIC_ID = Pattern.compile("\\d{16}");

    static final int RECENT_GAMES = 6;
    static final int SHELF_GAMES = 24;
    static final int PLAYING_GAMES = 12;

    private final PublicProfileQueryPort profiles;
    private final ShelfRepositoryPort shelves;
    private final ShelfContentQueryPort content;

    public ViewProfilesService(PublicProfileQueryPort profiles, ShelfRepositoryPort shelves,
                               ShelfContentQueryPort content) {
        this.profiles = profiles;
        this.shelves = shelves;
        this.content = content;
    }

    @Override
    public ProfileHeader header(long publicId) {
        return profiles.findHeader(publicId).orElseThrow(ProfileNotFoundException::new);
    }

    @Override
    @Transactional(readOnly = true)
    public PublicProfile profile(long publicId) {
        ProfileHeader header = header(publicId);
        long userId = profiles.findUserId(publicId).orElseThrow(ProfileNotFoundException::new);
        List<ShelfContent> filled = shelves.findAll(userId).stream()
                .map(shelf -> fill(userId, shelf))
                .flatMap(Optional::stream)
                .toList();
        return new PublicProfile(header, filled);
    }

    /** Empty for a collection that no longer exists (the shelf goes with it, but not in the same instant). */
    private Optional<ShelfContent> fill(long userId, Shelf shelf) {
        return switch (shelf.type()) {
            case COLLECTION -> content.collection(userId, shelf.collectionId(), SHELF_GAMES)
                    .map(c -> games(shelf, c.name(), c.games()));
            case RECENTLY_PLAYED -> Optional.of(games(shelf, null, content.recentlyPlayed(userId, RECENT_GAMES)));
            case PLAYING -> Optional.of(games(shelf, null, content.playing(userId, PLAYING_GAMES)));
            case COMPLETED -> Optional.of(games(shelf, null, content.completed(userId, SHELF_GAMES)));
            case ACHIEVEMENTS -> Optional.of(new ShelfContent(shelf.publicId(), shelf.type(), shelf.title(), null,
                    List.of(), content.achievements(userId, shelf.achievementIds()), null));
            case STATS -> Optional.of(new ShelfContent(shelf.publicId(), shelf.type(), shelf.title(), null,
                    List.of(), List.of(), content.stats(userId)));
        };
    }

    private static ShelfContent games(Shelf shelf, String collectionName, List<GameItem> games) {
        return new ShelfContent(shelf.publicId(), shelf.type(), shelf.title(), collectionName, games, List.of(), null);
    }

    @Override
    public List<ProfileHeader> search(String query, int page) {
        String q = query.trim();
        Long exactId = PUBLIC_ID.matcher(q).matches() ? Long.valueOf(q) : null;
        return profiles.search(q, exactId, PAGE_SIZE, (long) Math.max(0, page) * PAGE_SIZE);
    }
}
