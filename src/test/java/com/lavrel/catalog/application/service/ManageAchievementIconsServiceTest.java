package com.lavrel.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.lavrel.catalog.application.port.in.ManageAchievementIconsUseCase.IconReport;
import com.lavrel.catalog.application.port.in.ManageAchievementIconsUseCase.MirrorReport;
import com.lavrel.catalog.application.port.out.AchievementIconPort;
import com.lavrel.catalog.application.port.out.AchievementIconPort.IconRef;
import com.lavrel.catalog.application.port.out.GameMappingRepositoryPort;
import com.lavrel.catalog.application.port.out.IconFilesPort;
import com.lavrel.catalog.application.port.out.IconFilesPort.IconFile;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort;
import com.lavrel.catalog.application.port.out.PlatformCatalogPort.PlatformAchievement;
import com.lavrel.catalog.domain.GameMapping;
import com.lavrel.catalog.domain.GameNotFoundException;
import com.lavrel.shared.domain.Platform;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ManageAchievementIconsServiceTest {

    private final AchievementIconPort icons = mock(AchievementIconPort.class);
    private final IconFilesPort files = mock(IconFilesPort.class);
    private final GameMappingRepositoryPort mappings = mock(GameMappingRepositoryPort.class);
    private final PlatformCatalogPort steam = mock(PlatformCatalogPort.class);

    private ManageAchievementIconsService service(String base) {
        when(steam.platform()).thenReturn(Platform.STEAM);
        return new ManageAchievementIconsService(icons, files, mappings, List.of(steam), base);
    }

    @Test
    void theConfiguredHostAlwaysEndsWithASlash() {
        service("https://shared.fastly.steamstatic.com/community_assets/images/apps").useConfiguredImageHost();
        verify(icons).useSteamImageBase("https://shared.fastly.steamstatic.com/community_assets/images/apps/");
    }

    @Test
    void refreshUpdatesTheIconsWeHave() {
        ManageAchievementIconsService service = service("https://img/");
        when(mappings.findByGameIdAndPlatform(7L, Platform.STEAM))
                .thenReturn(Optional.of(new GameMapping(1L, 7L, Platform.STEAM, "2950790", "Game")));
        when(steam.fetchAchievements("2950790")).thenReturn(List.of(
                new PlatformAchievement("A1", "One", "", "https://old/images/apps/2950790/a.jpg", false),
                new PlatformAchievement("A2", "Two", "", "https://old/images/apps/2950790/b.jpg", false),
                new PlatformAchievement("A3", "No icon", "", null, false)));
        when(icons.updateFromSteam(7L, "A1", "https://old/images/apps/2950790/a.jpg")).thenReturn(true);
        when(icons.updateFromSteam(7L, "A2", "https://old/images/apps/2950790/b.jpg")).thenReturn(false);

        IconReport report = service.refreshFromSteam(7L);

        assertThat(report.updated()).isEqualTo(1);
        assertThat(report.unknown()).isEqualTo(1);
    }

    @Test
    void gamesWithoutSteamAreNotFound() {
        ManageAchievementIconsService service = service("https://img/");
        when(mappings.findByGameIdAndPlatform(9L, Platform.STEAM)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refreshFromSteam(9L)).isInstanceOf(GameNotFoundException.class);
        verify(icons, never()).updateFromSteam(anyLong(), anyString(), anyString());
    }

    @Test
    void mirrorCopiesWhatSteamStillHasAndReportsTheRest() {
        ManageAchievementIconsService service = service("https://img/");
        when(icons.findByGame(7L)).thenReturn(List.of(new IconRef(1L, "2950790/a.jpg", false),
                new IconRef(2L, "2950790/b.jpg", true), new IconRef(3L, "2950790/c.jpg", false)));
        IconFile file = new IconFile(new byte[] {1}, "image/jpeg");
        when(files.downloadFromSteam("2950790/a.jpg")).thenReturn(Optional.of(file));
        when(files.downloadFromSteam("2950790/c.jpg")).thenReturn(Optional.empty());
        when(files.publicUrl("2950790/a.jpg")).thenReturn("https://store/achievement-icons/2950790/a.jpg");

        MirrorReport report = service.mirror(7L);

        verify(files).store("2950790/a.jpg", file);
        verify(icons).useMirror(1L, "https://store/achievement-icons/2950790/a.jpg");
        assertThat(report.copied()).isEqualTo(1);
        assertThat(report.alreadyCopied()).isEqualTo(1);
        assertThat(report.missingAchievementIds()).containsExactly(3L);
    }

    @Test
    void unmirrorGoesBackToSteamAndDeletesOurCopies() {
        ManageAchievementIconsService service = service("https://img/");
        when(icons.findByGame(7L)).thenReturn(List.of(new IconRef(1L, "2950790/a.jpg", true),
                new IconRef(2L, "2950790/b.jpg", false)));

        assertThat(service.unmirror(7L)).isEqualTo(1);
        verify(icons).useSteam(1L);
        verify(files).delete("2950790/a.jpg");
        verify(files, never()).store(anyString(), any());
    }
}
