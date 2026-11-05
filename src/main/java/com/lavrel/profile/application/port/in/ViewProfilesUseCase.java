package com.lavrel.profile.application.port.in;

import com.lavrel.profile.domain.AvatarKind;
import com.lavrel.profile.domain.ProfileNotFoundException;
import com.lavrel.profile.domain.ShelfType;
import com.lavrel.shared.domain.Platform;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Public profiles as other people see them, and the player search. Only profiles their owner made public. */
public interface ViewProfilesUseCase {

    /** @throws ProfileNotFoundException if there is no public profile with that id */
    ProfileHeader header(long publicId);

    /** The header and every shelf with its content. @throws ProfileNotFoundException as header */
    PublicProfile profile(long publicId);

    /**
     * query: 2+ characters of a name, or an exact 16-digit public id.
     */
    List<ProfileHeader> search(String query, int page);

    /**
     * country and platforms are null / empty unless the user opted into showing them. avatar is the chosen or uploaded
     * picture only if the user opted in; otherwise GENERATED (the frontend draws it from the public id).
     */
    record ProfileHeader(long publicId, String username, YearMonth memberSince, Country country,
                         List<Platform> platforms, Avatar avatar) {
    }

    /** galleryId for GALLERY, imageUrl for UPLOADED; both null for GENERATED. */
    record Avatar(AvatarKind kind, String galleryId, String imageUrl) {

        public static final Avatar GENERATED = new Avatar(AvatarKind.GENERATED, null, null);
    }

    record Country(String code, String name) {
    }

    record PublicProfile(ProfileHeader header, List<ShelfContent> shelves) {
    }

    /**
     * One filled shelf. Depending on the type: games (COLLECTION, RECENTLY_PLAYED, PLAYING, COMPLETED; collectionName
     * for COLLECTION), achievements (ACHIEVEMENTS) or stats (STATS); the rest is empty or null.
     */
    record ShelfContent(UUID id, ShelfType type, String title, String collectionName, List<GameItem> games,
                        List<AchievementItem> achievements, Stats stats) {
    }

    record GameItem(long gameId, String title, String coverImageUrl) {
    }

    /** globalPercent: share of the game's owners who unlocked it (rarity); null while nobody owns the game. */
    record AchievementItem(long achievementId, String name, String iconUrl, long gameId, String gameTitle,
                           Double globalPercent, Instant unlockedAt) {
    }

    record Stats(int gamesOwned, int achievementsUnlocked, int gamesCompleted) {
    }
}
