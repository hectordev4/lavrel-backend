package com.lavrel.profile.adapter.in.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.lavrel.profile.application.port.in.ManageProfileUseCase;
import com.lavrel.profile.domain.ProfileSettings;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The user's own profile settings. Everything is off by default:
 *  - GET   /api/currentuser/profile
 *  - PATCH /api/currentuser/profile {"public": true, "showCountry": true, "showPlatforms": false, "showAvatar": true}
 *    (any subset; missing fields keep their value)
 */
@RestController
@RequestMapping("/api/currentuser/profile")
public class CurrentUserProfileController {

    private final ManageProfileUseCase profile;

    public CurrentUserProfileController(ManageProfileUseCase profile) {
        this.profile = profile;
    }

    @GetMapping
    public SettingsResponse get(@AuthenticationPrincipal AuthenticatedUser user) {
        return SettingsResponse.from(profile.settings(user.userId()));
    }

    @PatchMapping
    public SettingsResponse update(@AuthenticationPrincipal AuthenticatedUser user,
                                   @Valid @RequestBody SettingsRequest request) {
        return SettingsResponse.from(profile.update(user.userId(), request.isPublic(), request.showCountry(),
                request.showPlatforms(), request.showAvatar()));
    }

    /** "public" is a Java keyword, hence the accessor name; the JSON field is "public". */
    public record SettingsRequest(@JsonProperty("public") Boolean isPublic,
                                  Boolean showCountry, Boolean showPlatforms, Boolean showAvatar) {
    }

    public record SettingsResponse(@JsonProperty("public") boolean isPublic,
                                   boolean showCountry, boolean showPlatforms, boolean showAvatar) {

        static SettingsResponse from(ProfileSettings s) {
            return new SettingsResponse(s.profilePublic(), s.showCountry(), s.showPlatforms(), s.showAvatar());
        }
    }
}
