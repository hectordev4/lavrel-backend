package com.lavrel.devtools;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase.AuthenticatedUser;
import com.lavrel.user.application.port.in.ForceVerifyEmailUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * DEVELOPMENT ONLY. POST /api/dev/verify-my-email: see ForceVerifyEmailUseCase for the rules.
 * 200 verified; 404 not enabled for this account; 409 already verified; 502 Firebase failed.
 */
@RestController
@ConditionalOnProperty(name = "app.dev.verify-email-uids")
public class DevEmailVerificationController {

    private final ForceVerifyEmailUseCase forceVerifyEmail;

    public DevEmailVerificationController(ForceVerifyEmailUseCase forceVerifyEmail) {
        this.forceVerifyEmail = forceVerifyEmail;
    }

    @PostMapping("/api/dev/verify-my-email")
    public VerifiedResponse verifyMyEmail(@AuthenticationPrincipal AuthenticatedUser user) {
        forceVerifyEmail.verifyOwnEmail(user.firebaseUid(), user.emailVerified());
        return new VerifiedResponse(true,
                "Sign in again (or refresh the token) to get a token with email_verified = true");
    }

    public record VerifiedResponse(boolean emailVerified, String next) {
    }
}
