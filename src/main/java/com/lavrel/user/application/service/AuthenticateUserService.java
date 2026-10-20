package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase;
import com.lavrel.user.application.port.out.TokenVerifierPort;
import com.lavrel.user.application.port.out.TokenVerifierPort.VerifiedToken;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.User;
import com.lavrel.user.domain.Usernames;
import java.time.Clock;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private static final int MAX_NAME_ATTEMPTS = 10;

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

    /**
     * Keeps the private Google data (e-mail, photo) up to date. Only writes when it changed. The public
     * username is never touched here.
     */
    private User refreshProfile(User user, VerifiedToken token) {
        if (Objects.equals(user.email(), token.email()) && Objects.equals(user.photoUrl(), token.photoUrl())) {
            return user;
        }
        return users.save(user.withProfile(user.username(), token.email(), token.photoUrl()));
    }

    /** The public name is random on purpose: nothing from the Google account is shown to other people. */
    private User create(VerifiedToken token) {
        return users.save(new User(null, token.uid(), uniqueRandomUsername(), token.email(),
                token.photoUrl(), null, clock.instant()));
    }

    private String uniqueRandomUsername() {
        for (int attempt = 0; attempt < MAX_NAME_ATTEMPTS; attempt++) {
            String candidate = Usernames.random();
            if (!users.existsByUsername(candidate)) {
                return candidate;
            }
        }
        throw new IllegalStateException("Could not find a free random username");
    }
}
