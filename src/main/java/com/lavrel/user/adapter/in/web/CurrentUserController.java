package com.lavrel.user.adapter.in.web;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.in.DeleteAccountUseCase;
import com.lavrel.user.application.port.in.GetCurrentUserUseCase;
import com.lavrel.user.application.port.in.GetCurrentUserUseCase.CurrentUserView;
import com.lavrel.user.application.port.in.UpdateUserCountryUseCase;
import com.lavrel.user.application.port.in.UpdateUsernameUseCase;
import com.lavrel.user.domain.Role;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints about the user making the request (identified by the Firebase token). */
@RestController
@RequestMapping("/api/currentuser")
public class CurrentUserController {

    private final GetCurrentUserUseCase getCurrentUser;
    private final UpdateUserCountryUseCase updateUserCountry;
    private final UpdateUsernameUseCase updateUsername;
    private final DeleteAccountUseCase deleteAccount;

    public CurrentUserController(GetCurrentUserUseCase getCurrentUser,
                                 UpdateUserCountryUseCase updateUserCountry,
                                 UpdateUsernameUseCase updateUsername,
                                 DeleteAccountUseCase deleteAccount) {
        this.getCurrentUser = getCurrentUser;
        this.updateUserCountry = updateUserCountry;
        this.updateUsername = updateUsername;
        this.deleteAccount = deleteAccount;
    }

    /**
     * The user's own profile. email and photoUrl (from the sign-in account) are private: they are only
     * ever returned here, to the user themself. username is the public display name, random until the user
     * changes it; publicId is the number that identifies them to others. countryId is null until the user picks
     * a country. When emailVerified is false, this is the only endpoint the account can use: the frontend shows
     * "check your inbox", and once the link is clicked it refreshes the token (getIdToken(true)) and calls again.
     */
    @GetMapping
    public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        CurrentUserView view = getCurrentUser.getCurrentUser(user.userId());
        return new CurrentUserResponse(view.publicId(), view.firebaseUid(), view.username(), view.email(),
                view.emailVerified(), view.photoUrl(), view.countryId(), view.role());
    }

    /** Body {"countryId": 12} sets the country; {"countryId": null} (or {}) clears it. */
    @PatchMapping("/country")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCountry(@AuthenticationPrincipal AuthenticatedUser user,
                              @Valid @RequestBody UpdateCountryRequest request) {
        updateUserCountry.updateCountry(user.userId(), request.countryId());
    }

    /**
     * Body {"username": "Héctor K."}: a free display name (not unique), 2-32 characters, see Usernames.
     * The sign-up form of e-mail accounts calls this right after the account is created.
     */
    @PatchMapping("/username")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateUsername(@AuthenticationPrincipal AuthenticatedUser user,
                               @Valid @RequestBody UpdateUsernameRequest request) {
        updateUsername.updateUsername(user.userId(), request.username());
    }

    public record CurrentUserResponse(long publicId, String firebaseUid, String username, String email,
                                      boolean emailVerified, String photoUrl, Integer countryId, Role role) {
    }

    /**
     * DELETE /api/currentuser?confirm=true deletes the account for good: our data and the Firebase sign-in account.
     * Works before the e-mail is verified too. 204; 409 last_admin for the only admin.
     */
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccount(@AuthenticationPrincipal AuthenticatedUser user,
                              @RequestParam(name = "confirm", defaultValue = "false")
                              @AssertTrue(message = "add ?confirm=true to delete the account for good") boolean confirm) {
        deleteAccount.deleteAccount(user.userId());
    }

    /** countryId may be null (clears the country). */
    public record UpdateCountryRequest(@Positive Integer countryId) {
    }

    /** The rules (length, characters, reserved names) live in the domain: Usernames. */
    public record UpdateUsernameRequest(@NotNull String username) {
    }
}
