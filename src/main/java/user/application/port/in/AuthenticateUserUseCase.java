package user.application.port.in;

import user.domain.InvalidTokenException;

public interface AuthenticateUserUseCase {

    /**
     * Verifies the Firebase ID token and returns the matching user,
     * creating it on first login.
     *
     * @throws InvalidTokenException if the token is invalid or expired
     */
    AuthenticatedUser authenticate(String firebaseIdToken);

    /** What the security layer needs: only IDs, never the domain object. */
    record AuthenticatedUser(long userId, String firebaseUid) {
    }
}
