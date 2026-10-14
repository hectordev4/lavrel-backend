package com.lavrel.user.application.service;

import java.time.Clock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.lavrel.user.application.port.in.AuthenticateUserUseCase;
import com.lavrel.user.application.port.out.TokenVerifierPort;
import com.lavrel.user.application.port.out.TokenVerifierPort.VerifiedToken;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.User;

@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final TokenVerifierPort tokenVerifier;
    private final UserRepositoryPort users;
    private final Clock clock;

    public AuthenticateUserService(TokenVerifierPort tokenVerifier, UserRepositoryPort users, Clock clock) {
        this.tokenVerifier = tokenVerifier;
        this.users = users;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(String firebaseIdToken) {
        VerifiedToken token = tokenVerifier.verify(firebaseIdToken);
        User user = users.findByFirebaseUid(token.uid())
                .map(existing -> refreshProfile(existing, token))
                .orElseGet(() -> create(token));
        return new AuthenticatedUser(user.id(), user.firebaseUid());
    }

    /** Only writes when Firebase data actually changed, so normal requests do not hit the write path. */
    private User refreshProfile(User user, VerifiedToken token) {
        if (Objects.equals(user.email(), token.email()) && Objects.equals(user.photoUrl(), token.photoUrl())) {
            return user;
        }
        return users.save(user.withProfile(user.username(), token.email(), token.photoUrl()));
    }

    private User create(VerifiedToken token) {
        return users.save(new User(null, token.uid(), deriveUsername(token), token.email(),
                token.photoUrl(), null, clock.instant()));
    }

    private static String deriveUsername(VerifiedToken token) {
        if (token.name() != null && !token.name().isBlank()) {
            return token.name();
        }
        if (token.email() != null && token.email().contains("@")) {
            return token.email().substring(0, token.email().indexOf('@'));
        }
        return "player-" + token.uid().substring(0, Math.min(6, token.uid().length()));
    }
}
