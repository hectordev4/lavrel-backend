package com.lavrel.user.application.service;

import com.lavrel.user.application.port.in.AuthenticateUserUseCase;
import com.lavrel.user.application.port.out.TokenVerifierPort;
import com.lavrel.user.application.port.out.TokenVerifierPort.VerifiedToken;
import com.lavrel.user.application.port.out.UserRepositoryPort;
import com.lavrel.user.domain.User;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthenticateUserService implements AuthenticateUserUseCase {

    private final TokenVerifierPort tokenVerifier;
    private final UserRepositoryPort users;
    private final PublicIdAllocator publicIds;
    private final UsernameGenerator usernames;
    private final Clock clock;

    public AuthenticateUserService(TokenVerifierPort tokenVerifier, UserRepositoryPort users,
                                   PublicIdAllocator publicIds, UsernameGenerator usernames, Clock clock) {
        this.tokenVerifier = tokenVerifier;
        this.users = users;
        this.publicIds = publicIds;
        this.usernames = usernames;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthenticatedUser authenticate(String firebaseIdToken) {
        VerifiedToken token = tokenVerifier.verify(firebaseIdToken);
        User user = users.findByFirebaseUid(token.uid())
                .map(existing -> refreshProfile(existing, token))
                .orElseGet(() -> create(token));
        return new AuthenticatedUser(user.id(), user.firebaseUid(), user.role(), token.emailVerified());
    }

    /**
     * Keeps the sign-in data (e-mail, whether it is verified, photo) up to date. Only writes when it changed, so
     * the request after the user clicks the verification link (and refreshes the token) stores email_verified.
     * The public username is never touched here.
     */
    private User refreshProfile(User user, VerifiedToken token) {
        if (!user.signInDataDiffers(token.email(), token.emailVerified(), token.photoUrl())) {
            return user;
        }
        return users.save(user.withSignInData(token.email(), token.emailVerified(), token.photoUrl()));
    }

    /** The starting name is a random friendly one: nothing from the sign-in account is shown to other people. */
    private User create(VerifiedToken token) {
        return users.save(User.newcomer(publicIds.allocate(), token.uid(), usernames.generate(), token.email(),
                token.emailVerified(), token.photoUrl(), clock.instant()));
    }
}
