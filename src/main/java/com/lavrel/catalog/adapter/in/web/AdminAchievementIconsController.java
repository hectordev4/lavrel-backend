package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.ManageAchievementIconsUseCase;
import com.lavrel.catalog.application.port.in.ManageAchievementIconsUseCase.IconReport;
import com.lavrel.catalog.application.port.in.ManageAchievementIconsUseCase.MirrorReport;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin only. Achievement icons of one game:
 *  - POST   /api/admin/catalog/games/{gameId}/icons/refresh   read them from Steam again
 *  - POST   /api/admin/catalog/games/{gameId}/icons/mirror    copy them into our storage and serve them from there
 *  - DELETE /api/admin/catalog/games/{gameId}/icons/mirror    serve them from Steam again, delete our copies
 */
@RestController
@RequestMapping("/api/admin/catalog/games/{gameId}/icons")
public class AdminAchievementIconsController {

    private final ManageAchievementIconsUseCase icons;

    public AdminAchievementIconsController(ManageAchievementIconsUseCase icons) {
        this.icons = icons;
    }

    @PostMapping("/mirror")
    public MirrorReport mirror(@PathVariable("gameId") @Positive long gameId) {
        return icons.mirror(gameId);
    }

    @DeleteMapping("/mirror")
    public UnmirrorResponse unmirror(@PathVariable("gameId") @Positive long gameId) {
        return new UnmirrorResponse(icons.unmirror(gameId));
    }

    public record UnmirrorResponse(int servedFromSteamAgain) {
    }

    @PostMapping("/refresh")
    public IconReport refresh(@PathVariable("gameId") @Positive long gameId) {
        return icons.refreshFromSteam(gameId);
    }
}
