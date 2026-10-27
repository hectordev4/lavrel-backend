package com.lavrel.user.adapter.in.web;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.in.GetCurrentUserUseCase;
import com.lavrel.user.application.port.in.GetCurrentUserUseCase.CurrentUserView;
import com.lavrel.user.application.port.in.UpdateUserCountryUseCase;
import com.lavrel.user.application.port.in.UpdateUsernameUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints about the user making the request (identified by the Firebase token). */
@RestController
@RequestMapping("/api/currentuser")
public class CurrentUserController {

    private final GetCurrentUserUseCase getCurrentUser;
    private final UpdateUserCountryUseCase updateUserCountry;
    private final UpdateUsernameUseCase updateUsername;

    public CurrentUserController(GetCurrentUserUseCase getCurrentUser,
                                 UpdateUserCountryUseCase updateUserCountry,
                                 UpdateUsernameUseCase updateUsername) {
        this.getCurrentUser = getCurrentUser;
        this.updateUserCountry = updateUserCountry;
        this.updateUsername = updateUsername;
    }

    /**
     * The user's own profile. email and photoUrl (from the linked Google account) are private: they are only
     * ever returned here, to the user themself. username is the public name, random until the user changes it.
     * countryId is null until the user picks a country.
     */
    @GetMapping
    public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        CurrentUserView view = getCurrentUser.getCurrentUser(user.userId());
        return new CurrentUserResponse(view.userId(), view.firebaseUid(), view.username(), view.email(),
                view.photoUrl(), view.countryId());
    }

    /** Body {"countryId": 12} sets the country; {"countryId": null} (or {}) clears it. */
    @PatchMapping("/country")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCountry(@AuthenticationPrincipal AuthenticatedUser user,
                              @Valid @RequestBody UpdateCountryRequest request) {
        updateUserCountry.updateCountry(user.userId(), request.countryId());
    }

    /**
     * Body {"username": "my_name"}: 3-20 letters, digits or underscore, unique ignoring case.
     * The sign-up form of our own accounts calls this right after the account is created.
     */
    @PatchMapping("/username")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUsername(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody UpdateUsernameRequest request) {
        updateUsername.updateUsername(user.userId(), request.username());
    }

    public record CurrentUserResponse(long userId, String firebaseUid, String username, String email,
                                      String photoUrl, Integer countryId) {
    }

    /** countryId may be null (clears the country). */
    public record UpdateCountryRequest(@Positive Integer countryId) {
    }

    /** The format rules (3-20 letters, digits or underscore) live in the domain: Usernames. */
    public record UpdateUsernameRequest(@NotNull String username) {
    }
}
