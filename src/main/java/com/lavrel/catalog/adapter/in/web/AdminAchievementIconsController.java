package com.lavrel.catalog.adapter.in.web;

import com.lavrel.catalog.application.port.in.ManageAchievementIconsUseCase;
import com.lavrel.catalog.application.port.in.ManageAchievementIconsUseCase.IconReport;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin only. Achievement icons of one game:
 *  - POST /api/admin/catalog/games/{gameId}/icons/refresh   read them from Steam again
 */
@RestController
@RequestMapping("/api/admin/catalog/games/{gameId}/icons")
public class AdminAchievementIconsController {

    private final ManageAchievementIconsUseCase icons;

    public AdminAchievementIconsController(ManageAchievementIconsUseCase icons) {
        this.icons = icons;
    }

    @PostMapping("/refresh")
    public IconReport refresh(@PathVariable("gameId") @Positive long gameId) {
        return icons.refreshFromSteam(gameId);
    }
}
