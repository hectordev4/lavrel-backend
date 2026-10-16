package com.lavrel.user.adapter.in.web;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints about the user making the request (identified by the Firebase token).
 * The country update (PATCH /api/currentuser/country) will live here too.
 */
@RestController
@RequestMapping("/api/currentuser")
public class CurrentUserController {

    @GetMapping
    public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal AuthenticatedUser user) {
        return new CurrentUserResponse(user.userId(), user.firebaseUid());
    }

    public record CurrentUserResponse(long userId, String firebaseUid) {
    }
}
