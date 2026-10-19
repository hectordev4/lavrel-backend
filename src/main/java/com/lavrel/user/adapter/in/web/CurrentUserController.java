package com.lavrel.user.adapter.in.web;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.in.GetUserCountryUseCase;
import com.lavrel.user.application.port.in.UpdateUserCountryUseCase;
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

    private final GetUserCountryUseCase getUserCountry;
    private final UpdateUserCountryUseCase updateUserCountry;

    public CurrentUserController(GetUserCountryUseCase getUserCountry,
                                 UpdateUserCountryUseCase updateUserCountry) {
        this.getUserCountry = getUserCountry;
        this.updateUserCountry = updateUserCountry;
    }

    /** countryId is null until the user picks a country (it is optional). */
    @GetMapping
    public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return new CurrentUserResponse(user.userId(), user.firebaseUid(),
                getUserCountry.getCountryId(user.userId()));
    }

    /** Body {"countryId": 12} sets the country; {"countryId": null} (or {}) clears it. */
    @PatchMapping("/country")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateCountry(@AuthenticationPrincipal AuthenticatedUser user,
                              @RequestBody UpdateCountryRequest request) {
        updateUserCountry.updateCountry(user.userId(), request.countryId());
    }

    public record CurrentUserResponse(long userId, String firebaseUid, Integer countryId) {
    }

    public record UpdateCountryRequest(Integer countryId) {
    }
}
