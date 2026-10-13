package catalog.application.port.out;

import java.util.List;
import java.util.Optional;
import shared.domain.Platform;

/** Implemented by the platform clients (SteamClient, XboxClient, PsnClient). */
public interface PlatformCatalogPort {

    Platform platform();

    Optional<PlatformGameInfo> fetchGameInfo(String platformGameId);

    List<PlatformAchievement> fetchAchievements(String platformGameId);

    record PlatformGameInfo(String platformGameId, String title, String coverImageUrl) {
    }

    record PlatformAchievement(String platformAchievementId, String name, String description,
                               String iconUrl, boolean hidden) {
    }
}
