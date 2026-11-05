package com.lavrel.profile.adapter.in.web;

import com.lavrel.profile.application.port.in.ViewProfilesUseCase;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.Country;
import com.lavrel.profile.application.port.in.ViewProfilesUseCase.ProfileHeader;
import com.lavrel.shared.domain.Platform;
import com.lavrel.user.domain.PublicIds;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.YearMonth;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, no login needed. Only profiles their owner made public:
 *  - GET /api/users/{publicId}   the profile (404 profile_not_found for private or unknown ones, not told apart)
 *  - GET /api/users?q=           player search by name (2+ characters) or an exact 16-digit public id
 */
@RestController
public class PublicProfileController {

    private final ViewProfilesUseCase profiles;

    public PublicProfileController(ViewProfilesUseCase profiles) {
        this.profiles = profiles;
    }

    @GetMapping("/api/users/{publicId}")
    public ProfileResponse profile(@PathVariable("publicId") @Min(PublicIds.MIN) @Max(PublicIds.MAX) long publicId) {
        return ProfileResponse.from(profiles.header(publicId));
    }

    @GetMapping("/api/users")
    public PlayersResponse search(@RequestParam("q") @NotNull @Size(min = 2, max = 50) String q,
                                  @RequestParam(name = "page", defaultValue = "0") @Min(0) @Max(100) int page) {
        return new PlayersResponse(profiles.search(q, page).stream().map(ProfileResponse::from).toList(), page);
    }

    public record PlayersResponse(List<ProfileResponse> players, int page) {
    }

    /** country and platforms only when the user opted in; platforms are badges, never account names or ids. */
    public record ProfileResponse(long publicId, String username, YearMonth memberSince, Country country,
                                  List<Platform> platforms) {

        static ProfileResponse from(ProfileHeader h) {
            return new ProfileResponse(h.publicId(), h.username(), h.memberSince(), h.country(), h.platforms());
        }
    }
}
