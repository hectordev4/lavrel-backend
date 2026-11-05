package com.lavrel.user.adapter.in.web;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.in.AutoSyncSettingUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET / PUT /api/currentuser/auto-sync {"enabled": false}: whether the user's accounts are synced automatically
 * (every few hours, only games played since the last sync). On by default.
 */
@RestController
@RequestMapping("/api/currentuser/auto-sync")
public class AutoSyncSettingController {

    private final AutoSyncSettingUseCase setting;

    public AutoSyncSettingController(AutoSyncSettingUseCase setting) {
        this.setting = setting;
    }

    @GetMapping
    public AutoSyncResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return new AutoSyncResponse(setting.isEnabled(user.userId()));
    }

    @PutMapping
    public AutoSyncResponse set(@AuthenticationPrincipal AuthenticatedUser user,
                                @Valid @RequestBody AutoSyncRequest request) {
        setting.setEnabled(user.userId(), request.enabled());
        return new AutoSyncResponse(request.enabled());
    }

    public record AutoSyncRequest(@NotNull Boolean enabled) {
    }

    public record AutoSyncResponse(boolean enabled) {
    }
}
